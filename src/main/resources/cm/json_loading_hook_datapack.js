function initializeCoreMod() {
    var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
    var MethodNode = Java.type('org.objectweb.asm.tree.MethodNode');
    var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
    var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var InsnList = Java.type('org.objectweb.asm.tree.InsnList');

    function addAll(insList) {
        var i;
        for(i =1;i<arguments.length;i++)
        {
            insList.add(arguments[i]);
        }
    }

    function label() { return new LabelNode(); }

    function aload(n) { return new VarInsnNode(Opcodes.ALOAD, n); }
    function aload0() { return new VarInsnNode(Opcodes.ALOAD, 0); }
    function aload1() { return new VarInsnNode(Opcodes.ALOAD, 1); }

    function astore(n) { return new VarInsnNode(Opcodes.ASTORE, n); }
    function astore0() { return new VarInsnNode(Opcodes.ASTORE, 0); }
    function astore1() { return new VarInsnNode(Opcodes.ASTORE, 1); }

    function invokeSpecial(ownerClass, method, signature, isInterface) {
        return new MethodInsnNode(
            Opcodes.INVOKESPECIAL, ownerClass, method, signature, isInterface
        );
    }

    function invokeStatic(ownerClass, method, signature, isInterface) {
        return new MethodInsnNode(
            Opcodes.INVOKESTATIC, ownerClass, method, signature, isInterface
        );
    }

    function ifEq(lbl) {
        return new JumpInsnNode(Opcodes.IFEQ, lbl);
    }

    function ifNonnull(lbl) {
        return new JumpInsnNode(Opcodes.IFNONNULL, lbl);
    }

    function ret() { return new InsnNode(Opcodes.RETURN); }

    function getField(ownerClass, fieldName, fieldDescriptor, isStatic) {
        return new FieldInsnNode(isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD, cn.name, fieldName, fieldDescriptor);
    }

	return {
		'JsonMerger JsonReloadListener#prepare Transformer': {
			'target': {
				'type': 'CLASS',
				'name': "net.minecraft.client.resources.JsonReloadListener"
			},
			'transformer': function(classNode) {

                // Step 0: Find method
                var prepareMethodName = ASMAPI.mapMethod("func_212854_a_");

                var method = null;
                var m;
                for(m = 0; m < classNode.methods.size(); m++)
                {
                    var mn = classNode.methods.get(m);
                    if(mn.name.equals(prepareMethodName) && mn.desc.equals("(Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)Ljava/util/Map;"))
                    {
                        method = mn;
                        break;
                    }
                }

                if (method == null)
                {
                    throw new Error("Could not find method?!");
                }

                // Part 1: Find method call to fromJson, and inject right before this code segment.
                //
                //   INVOKESTATIC net/minecraft/util/JSONUtils.fromJson (Lcom/google/gson/Gson;Ljava/io/Reader;Ljava/lang/Class;)Ljava/lang/Object;
                //
                var fromJsonMethod = ASMAPI.mapMethod("func_193839_a");

                // Find method call instruction
                var methodCallFoundAt0 = -1;
                var i;
                for(i=0; i < method.instructions.size(); i++)
                {
                    var insn = method.instructions.get(i);
                    if (insn instanceof MethodInsnNode &&
                        insn.getOpcode() == Opcodes.INVOKESTATIC &&
                        insn.owner.equals("net/minecraft/util/JSONUtils") &&
                        insn.name.equals(fromJsonMethod))
                    {
                        methodCallFoundAt0 = i;
                        break;
                    }
                }

                if (methodCallFoundAt0 < 0)
                {
                    throw new Error("Could not find method call?!");
                }

                // Find LabelNode above method call
                var labelFoundAt0 = -1;
                for(i=methodCallFoundAt0;i>=0;i--)
                {
                    var insn = method.instructions.get(i);
                    if (insn instanceof LabelNode)
                    {
                        labelFoundAt0 = i;
                        break;
                    }
                }

                if (labelFoundAt0 < 0)
                {
                    throw new Error("Could not find label above?!");
                }

                // Find LabelNode below method call
                var labelFoundAt1 = -1;
                for(i=methodCallFoundAt0; i < method.instructions.size(); i++)
                {
                    var insn = method.instructions.get(i);
                    if (insn instanceof LabelNode)
                    {
                        labelFoundAt1 = i;
                        break;
                    }
                }

                if (labelFoundAt1 < 0)
                {
                    throw new Error("Could not find label below?!");
                }

                // Target label for the IFNONNULL
                var labelEndIf = method.instructions.get(labelFoundAt1);

                // Prepare list of instructions to insert
                var list0 = new InsnList();
                addAll(list0,

                    label(),
                        aload(1),
                        aload(6),
                        invokeStatic("dev/gigaherz/jsonmerger/JsonMerger", "combineAllJsonResources",
                            "(Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/util/ResourceLocation;)Lcom/google/gson/JsonElement;", false
                        ),
                        astore(15),

                    label(),
                        aload(15),
                        ifNonnull(labelEndIf)
                );

                // Insert instructions at the target location
                var target0 = method.instructions.get(labelFoundAt0);
                method.instructions.insertBefore(target0, list0);
/* AFTER

  L3
    LINENUMBER 49 L3
    ALOAD 1
    ALOAD 6
    INVOKESTATIC net/minecraftforge/common/util/JsonMerger.combineAllJsonResources (Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/util/ResourceLocation;)Lcom/google/gson/JsonElement;
    ASTORE 15
   L46
    LINENUMBER 50 L46
    ALOAD 15
    IFNONNULL L47

    ... original code ...
   L48
    LINENUMBER 51 L48
    ALOAD 0
    GETFIELD net/minecraft/client/resources/JsonReloadListener.gson : Lcom/google/gson/Gson;
    ALOAD 13
    LDC Lcom/google/gson/JsonElement;.class
    INVOKESTATIC net/minecraft/util/JSONUtils.fromJson (Lcom/google/gson/Gson;Ljava/io/Reader;Ljava/lang/Class;)Ljava/lang/Object;
    CHECKCAST com/google/gson/JsonElement
    ASTORE 15
   L47
*/
/* BEFORE
   L3
    LINENUMBER 49 L3
    ALOAD 0
    GETFIELD net/minecraft/client/resources/JsonReloadListener.gson : Lcom/google/gson/Gson;
    ALOAD 13
    LDC Lcom/google/gson/JsonElement;.class
    INVOKESTATIC net/minecraft/util/JSONUtils.fromJson (Lcom/google/gson/Gson;Ljava/io/Reader;Ljava/lang/Class;)Ljava/lang/Object;
    CHECKCAST com/google/gson/JsonElement
    ASTORE 15
   L46
*/

				return classNode;
			}
		}
	}
}
