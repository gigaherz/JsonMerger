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
        return new FieldInsnNode(isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD, ownerClass, fieldName, fieldDescriptor);
    }

	return {
		'JsonMerger WorldSettingsImport.IResourceAccess#func_244345_a Transformer': {
			'target': {
				'type': 'CLASS',
				'name': "net/minecraft/util/registry/WorldSettingsImport$IResourceAccess$1"
			},
			'transformer': function(classNode) {

                // Step 0: Find method
                var prepareMethodName = ASMAPI.mapMethod("func_241879_a");

                var method = null;
                var m;
                for(m = 0; m < classNode.methods.size(); m++)
                {
                    var mn = classNode.methods.get(m);
                    if(mn.name.equals(prepareMethodName) && mn.desc.equals("(Lcom/mojang/serialization/DynamicOps;Lnet/minecraft/util/RegistryKey;Lnet/minecraft/util/RegistryKey;Lcom/mojang/serialization/Decoder;)Lcom/mojang/serialization/DataResult;"))
                    {
                        method = mn;
                        break;
                    }
                }

                if (method == null)
                {
                    throw new Error("Could not find method?!");
                }

                // Part 1: Find method call to parse, and inject right before this code segment.
                //
                //   INVOKEVIRTUAL com/google/gson/JsonParser.parse (Ljava/io/Reader;)Lcom/google/gson/JsonElement;
                //

                // Find method call instruction
                var methodCallFoundAt0 = -1;
                var i;
                for(i=0; i < method.instructions.size(); i++)
                {
                    var insn = method.instructions.get(i);
                    if (insn instanceof MethodInsnNode &&
                        insn.getOpcode() == Opcodes.INVOKEVIRTUAL &&
                        insn.owner.equals("com/google/gson/JsonParser") &&
                        insn.name.equals("parse"))
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
                        aload(0),
                        getField("net/minecraft/util/registry/WorldSettingsImport$IResourceAccess$1", "val$p_244345_0_", "Lnet/minecraft/resources/IResourceManager;"),
                        aload(6),
                        invokeStatic("dev/gigaherz/jsonmerger/JsonMerger", "combineAllJsonResources",
                            "(Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/util/ResourceLocation;)Lcom/google/gson/JsonElement;", false
                        ),
                        astore(12),

                    label(),
                        aload(12),
                        ifNonnull(labelEndIf)
                );

                // Insert instructions at the target location
                var target0 = method.instructions.get(labelFoundAt0);
                method.instructions.insertBefore(target0, list0);
/* AFTER

   L29
    LINENUMBER 193 L29
    ALOAD 0
    GETFIELD net/minecraft/util/registry/WorldSettingsImport$IResourceAccess$1.val$p_244345_0_ : Lnet/minecraft/resources/IResourceManager;
    ALOAD 6
    INVOKESTATIC net/minecraftforge/common/util/JsonMerger.combineAllJsonResources (Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/util/ResourceLocation;)Lcom/google/gson/JsonElement;
    ASTORE 12
   L30
    LINENUMBER 194 L30
    ALOAD 12
    IFNONNULL L31
*/

				return classNode;
			}
		}
	}
}
