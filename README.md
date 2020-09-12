Json Merger
=============

A small server-only (works on clients) mod that enables loading a combined version of the jsons,
for json files that are normally "last man wins".
This is done through a patching mechanism, where jsons can manually specify they should act in "merge" mode instead of
the default "overwrite" mode. Fine-tuning can be achieved by specifying merge/overwrite/delete in the individual children.

See the [How to Use](#how-to-use-it) section below, for more details.

## What this mod it allows

This mod enables merging for:
* Loot tables
* Advancements
* Recipes
* Biomes*
* Dimensions*
* Structures*

\* This mod CANNOT modify the default dimension/biome/structure definitions, only allows merging between json files. 
This means if you want to patch the vanilla defaults, you will need to first export the vanilla defaults to json (or use an existing vanilla-defaults datapack),
and then provide the patch files in your datapack.  

## How it works

Json Merger has a small coremod that patches two method calls:

1. The call to `JSONUtils.fromJson` in `JsonReloadListener#prepare`, which handles loading datapack jsons such as loot tables.
2. The call to `JsonParser#prepare` in the `func_241879_a` method from the anonymous impl of `WorldSettingsImport.IResourceAccess` contained in `func_244345_a`,
 which handles loading the json files for worldgen settings.

## How to use it

This feature is available on specific supported json files (eg, loot tables).

Json objects can contain a "_jm_combine" key, which should itself be a json object. 

By default, all json files start in "overwrite" mode, which returns the file as-is, maintaining the default vanilla behaviour. 

Certain alternative modes are available depending on the element type.

### Objects:

- Combine: The two objects are combined, processing the child elements recursively.

### Arrays:

- Combine: The two arrays are combined, processing the child elements recursively. When in combine mode, array elements can have special modes to choose what to replace, or where to insert.
- Zip: The two arrays are combined pairwise, combining each pair of elements into a new element.

### Child elements within arrays:

- Append (default): The element is added to the end of the list.
- Overwrite: The element with either the specified index, or the specified search pattern, gets replaced.
- Combine: Same as replace, but combines the existing value instead of overwriting.
- Insert: The element is inserted at the position with the specified index.

### How it's processed

The process begins at the root object, which defaults to "overwrite". If this object is set to "combine" instead, the merger will iterate through the key sets of both the original and the new, combining values for keys that exist on both objects. Children of combined objects inherit the combine mode, but they can choose to overwrite instead, by specifying this explicitly.

For arrays, the combine operation adds elements to the array, without combining them. However those child elements can specify their own mode, in which case the element will either be inserted at a chosen location, will replace (overwrite) and existing value, or will be combined with an existing value. This behaviour is guided either by index, or by search pattern.

For all other data types (primitives, null), overwrite and combine act the same: the element is returned as-is.

### Controlling the insertion behaviour for non-object elements

Since only json objects can contain the "_jm_combine" key, another feature is available: any object which has the "_jm_combine" key, and inside has a "value" key, the contents of this value key are used, instead of the object. This allows inserting/replacing arrays, strings, or other primitives, contained within other arrays, or controlling the mode of values in individual keys within an object.

That is, the following object: 

```json
{
  "_jm_combine": { "value": 1 }
}
```

is equivalent to the primitive `1`. 