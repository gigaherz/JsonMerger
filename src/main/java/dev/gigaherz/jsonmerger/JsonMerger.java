package dev.gigaherz.jsonmerger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Mod("jsonmerger")
public class JsonMerger
{
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Gson SERIALIZER = new GsonBuilder().create();

    public static JsonElement combineAllJsonResources(ResourceManager resourceManager, ResourceLocation location)
    {
        try
        {
            List<Resource> allResources = resourceManager.getResources(location);
            List<Resource> resourceList = allResources.size() == 1 ? allResources : Lists.reverse(allResources);
            List<JsonElement> results = Lists.newArrayList();
            boolean stopping = false;
            for (Resource resource : resourceList)
            {
                if (stopping)
                {
                    // needed to avoid leaking file handles.
                    resource.close();
                    continue;
                }
                try (Resource t = resource;
                     InputStream is = t.getInputStream();
                     InputStreamReader rd = new InputStreamReader(is))
                {
                    JsonElement jsonElement = SERIALIZER.fromJson(rd, JsonElement.class);
                    if (jsonElement == null)
                    {
                        LOGGER.error("Couldn't load data from {} as it's null or empty", location);
                        return null;
                    }

                    results.add(jsonElement);

                    if (isOverwriteMode(jsonElement))
                        stopping = true;
                }
                catch (IOException e)
                {
                    LOGGER.error("Error reading JSON from {}", location, e);
                }
            }

            return combineStack(results.size() == 1 ? results : Lists.reverse(results));
        }
        catch (IOException e)
        {
            throw new RuntimeException(String.format("Failed to close file stream for %s", location), e);
        }
    }

    public static JsonElement combineStack(List<JsonElement> elements)
    {
        JsonElement merged = null;
        for (JsonElement next : elements)
        {
            if (merged == null)
            {
                merged = cleanup(next);
            }
            else
            {
                merged = combineJsonElements(merged, next, MergeMode.OVERWRITE);
            }
        }
        return merged;
    }

    private static JsonElement combineJsonElements(JsonElement first, JsonElement second, MergeMode parentMode)
    {
        MergeMode mode = parentMode;
        JsonObject settings;
        if (second.isJsonObject())
        {
            JsonObject secondObj = second.getAsJsonObject();
            if (secondObj.has("_jm_combine"))
            {
                settings = secondObj.getAsJsonObject("_jm_combine");

                if (settings.has("mode"))
                {
                    String modeName = settings.get("mode").getAsString();
                    mode = MergeMode.byName(modeName);
                    if (mode == null)
                        throw new JsonSyntaxException(String.format("Unknown combine mode: %s", modeName));
                }

                if (settings.has("value"))
                {
                    second = settings.get("value");

                    for (Map.Entry<String, JsonElement> child : secondObj.entrySet())
                    {
                        if (!child.getKey().equals("_jm_combine"))
                            throw new JsonSyntaxException("Objects must not have content other than '_jm_combine' if they use the 'value' feature.");
                    }
                }
            }
        }

        return getCombinedInternal(first, second, mode);
    }

    private static JsonElement getCombinedInternal(JsonElement first, JsonElement second, MergeMode mode)
    {
        if (mode == MergeMode.OVERWRITE)
        {
            return cleanup(second);
        }

        if (first.isJsonObject())
        {
            if (!second.isJsonObject())
            {
                throw new JsonSyntaxException("The combining counterpart for a json object must be another json object");
            }

            if (mode == MergeMode.COMBINE)
            {
                return combineObjects(first.getAsJsonObject(), second.getAsJsonObject());
            }

            throw new JsonSyntaxException(String.format("Invalid combine mode for a json object: %s. Allowed: overwrite, combine", mode));
        }
        else if (first.isJsonArray())
        {
            return switch (mode)
                    {
                        case COMBINE -> concatenateArrays(first.getAsJsonArray(), second.getAsJsonArray());
                        case ZIP -> zipArrays(first.getAsJsonArray(), second.getAsJsonArray());
                        default -> throw new JsonSyntaxException(String.format("Invalid combine mode for a json array: %s. Allowed: overwrite, combine, zip", mode));
                    };
        }
        else if (first.isJsonNull())
        {
            if (mode != MergeMode.COMBINE)
                throw new JsonSyntaxException(String.format("Invalid combine mode for a json null: %s", mode));
        }
        else
        {
            if (mode != MergeMode.COMBINE)
                throw new JsonSyntaxException(String.format("Invalid combine mode for a json primitive: %s", mode));
        }

        return second;
    }

    private static JsonElement cleanup(JsonElement second)
    {
        if (second.isJsonObject())
        {
            var obj = second.getAsJsonObject();
            if (obj.has("_jm_combine"))
            {
                var settings = obj.get("_jm_combine").getAsJsonObject();
                if (settings.has("value"))
                {
                    return cleanup(settings.get("value"));
                }
                obj.remove("_jm_combine");
            }
            for(var kv : obj.entrySet())
            {
                var v = kv.getValue();
                var r = cleanup(v);
                if (r != v)
                    kv.setValue(r);
            }
        }
        else if(second.isJsonArray())
        {
            var arr = second.getAsJsonArray();
            for(int i=0;i<arr.size();i++)
            {
                var v = arr.get(i);
                var r = cleanup(v);
                if (r != v)
                {
                    arr.set(i, r);
                }
            }
        }
        return second;
    }

    public static JsonElement combineObjects(JsonObject first, JsonObject second)
    {
        JsonObject result = new JsonObject();
        List<String> keysToDelete = Lists.newArrayList("_jm_combine");
        for (Map.Entry<String, JsonElement> entry : first.entrySet())
        {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (second.has(key))
            {
                JsonElement secondElement = second.get(key);
                if (getMode(secondElement, MergeMode.COMBINE) == MergeMode.DELETE)
                    keysToDelete.add(key);
                else
                    result.add(key, combineJsonElements(value, secondElement, MergeMode.COMBINE));
            }
            else
            {
                result.add(key, value);
            }
        }
        for (Map.Entry<String, JsonElement> entry : second.entrySet())
        {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (!result.has(key))
            {
                value = cleanup(value);
                result.add(key, value);
            }
        }
        for (String key : keysToDelete)
        {
            result.remove(key);
        }
        return result;
    }

    private static JsonElement zipArrays(JsonArray first, JsonArray second)
    {
        if (first.size() == 0)
        {
            return cleanup(second);
        }

        if (second.size() == 0)
            return first;

        JsonArray array = new JsonArray();
        int end = Math.min(first.size(), second.size());
        for (int i = 0; i < end; i++)
        {
            array.add(combineJsonElements(first.get(i), second.get(i), MergeMode.COMBINE));
        }

        if (first.size() > end)
        {
            for(int i=end; i<first.size();i++)
            {
                array.add(first.get(i));
            }
        }
        else
        {
            for(int i=end; i<second.size();i++)
            {
                var value = second.get(i);
                value = cleanup(value);
                array.add(value);
            }
        }

        return array;
    }

    private static JsonElement concatenateArrays(JsonArray first, JsonArray second)
    {
        if (first.size() == 0)
        {
            return cleanup(second);
        }

        if (second.size() == 0)
            return first;

        List<JsonElement> elements = Lists.newArrayList();
        for (int i = 0; i < first.size(); i++)
        {
            elements.add(first.get(i));
        }
        for (int i = 0; i < second.size(); i++)
        {
            var s = second.get(i);
            processJsonArrayElement(elements, s);
        }

        JsonArray result = new JsonArray();
        for (JsonElement element : elements)
        {
            result.add(element);
        }
        return result;
    }

    private static void processJsonArrayElement(List<JsonElement> parent, JsonElement second)
    {
        MergeMode mode = MergeMode.APPEND;
        JsonObject settings;
        Integer index = null;
        JsonElement find = null;
        boolean silent = false;
        boolean exact = false;
        boolean after = false;
        if (second.isJsonObject())
        {
            JsonObject secondObj = second.getAsJsonObject();
            if (secondObj.has("_jm_combine"))
            {
                settings = secondObj.getAsJsonObject("_jm_combine");

                if (settings.has("mode"))
                {
                    String modeName = settings.get("mode").getAsString();
                    mode = MergeMode.byName(modeName);
                    if (mode == null)
                        throw new JsonSyntaxException(String.format("Unknown combine mode: %s", modeName));
                }

                if (settings.has("value"))
                {
                    second = settings.get("value");
                }

                if (settings.has("index"))
                {
                    if (mode == MergeMode.APPEND)
                        throw new JsonSyntaxException("Index is not available in append mode.");
                    index = settings.get("index").getAsInt();
                }

                if (settings.has("find"))
                {
                    if (index != null)
                        throw new JsonSyntaxException("Array replace option 'find' can not be used at the same time as 'index'.");
                    if (mode == MergeMode.APPEND)
                        throw new JsonSyntaxException("Find is not available in append mode.");
                    find = settings.get("find");
                }

                if (settings.has("silent"))
                    silent = settings.get("silent").getAsBoolean();

                if (settings.has("exact"))
                {
                    exact = settings.get("exact").getAsBoolean();
                    if (exact && find == null)
                        throw new JsonSyntaxException("Exact is only available when using find.");
                }

                if (settings.has("after"))
                {
                    after = settings.get("after").getAsBoolean();

                    if (after && mode != MergeMode.INSERT)
                        throw new JsonSyntaxException("After is only available in insert mode.");
                }

                secondObj.remove("_jm_combine");
            }
        }

        switch (mode)
        {
            case APPEND -> parent.add(second);
            case INSERT -> processArrayOperation(parent, second, index, find, exact, silent, after, parent::add);
            case OVERWRITE -> processArrayOperation(parent, second, index, find, exact, silent, after, parent::set);
            case COMBINE -> processArrayOperation(parent, second, index, find, exact, silent, after, (i, e) ->
                    parent.set(i, combineJsonElements(parent.get(i), e, MergeMode.COMBINE)));
            case DELETE -> processArrayOperation(parent, second, index, find, exact, silent, after, (i, e) ->
                    parent.remove((int) i));
            default -> throw new JsonSyntaxException(String.format("Invalid combine mode for children of json arrays: %s. Allowed: append, combine, overwrite, insert", mode));
        }
    }

    private static void processArrayOperation(List<JsonElement> parent, JsonElement second,
                                              Integer index, JsonElement find, boolean exact, boolean silent,
                                              boolean after, BiConsumer<Integer, JsonElement> operation)
    {
        if (index == null && find == null)
            throw new JsonSyntaxException("Modes other than append mode must specify an 'index' or a 'find' search pattern.");
        if (index != null)
        {
            if (after)
                throw new JsonSyntaxException("After is not available when index is used.");
            operation.accept(index, second);
        }
        else
        {
            boolean found = false;
            for (int i = 0; i < parent.size(); i++)
            {
                if (compareWithPattern(parent.get(i), find, exact))
                {
                    if (after) i++;
                    operation.accept(i, second);
                    found = true;
                    break;
                }
            }
            if (!found && !silent)
            {
                LOGGER.warn("Search pattern did not find a target while looking for: {}", find.toString());
            }
        }
    }

    private static boolean compareWithPattern(JsonElement base, JsonElement pattern, boolean exact)
    {
        if (pattern.isJsonObject())
        {
            if (!base.isJsonObject())
                return false;
            return compareObjectWithPattern(base.getAsJsonObject(), pattern.getAsJsonObject(), exact);
        }
        else if (pattern.isJsonArray())
        {
            if (!base.isJsonArray())
                return false;
            return compareArrayWithPattern(base.getAsJsonArray(), pattern.getAsJsonArray(), exact);
        }
        else
        {
            return base.equals(pattern);
        }
    }

    private static boolean compareArrayWithPattern(JsonArray array, JsonArray pattern, boolean exact)
    {
        if (exact)
        {
            if (array.size() != pattern.size())
                return false;

            for (int i = 0; i < pattern.size(); i++)
            {
                if (!compareWithPattern(array.get(i), pattern.get(i), true))
                    return false;
            }

            return true;
        }

        if (pattern.size() > array.size())
            return false;

        BitSet processedIndices = new BitSet();
        for (int i = 0; i < pattern.size(); i++)
        {
            boolean found = false;
            for (int j = 0; j < array.size(); j++)
            {
                if (processedIndices.get(j))
                    continue;

                if (compareWithPattern(array.get(j), pattern.get(i), false))
                {
                    processedIndices.set(j);
                    found = true;
                    break;
                }
            }

            if (!found)
                return false;
        }

        return true;
    }

    private static boolean compareObjectWithPattern(JsonObject obj, JsonObject pattern, boolean exact)
    {
        for (Map.Entry<String, JsonElement> entry : pattern.entrySet())
        {
            String key = entry.getKey();
            if (!obj.has(key))
                return false;
            if (!compareWithPattern(obj.get(key), entry.getValue(), exact))
                return false;
        }

        if (exact)
        {
            for (Map.Entry<String, JsonElement> entry : pattern.entrySet())
            {
                String key = entry.getKey();
                if (!pattern.has(key))
                    return false;
            }
        }

        return true;
    }

    private static boolean isOverwriteMode(JsonElement jsonElement)
    {
        return getMode(jsonElement, MergeMode.OVERWRITE) == MergeMode.OVERWRITE;
    }

    private static MergeMode getMode(JsonElement jsonElement, MergeMode fallback)
    {
        if (!jsonElement.isJsonObject())
            return fallback;
        JsonObject obj = jsonElement.getAsJsonObject();
        if (!obj.has("_jm_combine"))
            return fallback;
        JsonObject settings = obj.getAsJsonObject("_jm_combine");
        if (!settings.has("mode"))
            return fallback;
        String modeName = settings.get("mode").getAsString();
        MergeMode mode = MergeMode.byName(modeName);
        if (mode == null)
            throw new JsonSyntaxException(String.format("Unknown combine mode: %s", modeName));
        return mode;
    }

    public enum MergeMode
    {
        OVERWRITE("overwrite"), /* default vanilla behaviour. CHILDREN OF ARRAYS: replaces */

        COMBINE("combine"), /* OBJECTS: combines values with same key; ARRAYS: inserts, replaces or appends the elements of the second array into the first */

        ZIP("zip"), /* ARRAYS ONLY: zips the arrays, combining each pair of elements */

        APPEND("append"), /* CHILDREN OF ARRAYS ONLY: inserts the element at the end of the array (DEFAULT) */
        INSERT("insert"),/* CHILDREN OF ARRAYS ONLY: inserts the element at the specified index */

        DELETE("delete"); /* CHILDREN OF ARRAYS AND OBJECTS: deletes the specified element */

        private final String name;

        MergeMode(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }

        public static final ImmutableList<MergeMode> VALUES = ImmutableList.copyOf(values());

        @Nullable
        public static MergeMode byName(String name)
        {
            for (MergeMode m : VALUES)
            {
                if (m.getName().equals(name))
                    return m;
            }
            return null;
        }
    }
}
