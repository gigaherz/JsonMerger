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
				'name': "net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener"
			},
			'transformer': function(classNode) {

                // Step 0: Find method
                var prepareMethodName = ASMAPI.mapMethod("m_5944_"); // SimplePreparableReloadListener.prepare

                var method = null;
                var m;
                for(m = 0; m < classNode.methods.size(); m++)
                {
                    var mn = classNode.methods.get(m);
                    if(mn.name.equals(prepareMethodName) && mn.desc.equals("(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/Map;"))
                    {
                        method = mn;
                        break;
                    }
                }

                if (method == null)
                {
                    ASMAPI.log("ERROR", "Could not find SimplePreparableReloadListener.prepare");
                    throw new Error("Could not find method?!");
                }

                ASMAPI.log("INFO", "Found SimplePreparableReloadListener.prepare");

                // Part 1: Find method call to fromJson, and inject right before this code segment.
                //
                //   INVOKESTATIC net/minecraft/util/GsonHelper.fromJson (Lcom/google/gson/Gson;Ljava/io/Reader;Ljava/lang/Class;)Ljava/lang/Object;
                //
                var fromJsonMethod = ASMAPI.mapMethod("m_13776_");

                // Find method call instruction
                var methodCallFoundAt0 = -1;
                var i;
                for(i=0; i < method.instructions.size(); i++)
                {
                    var insn = method.instructions.get(i);
                    if (insn instanceof MethodInsnNode &&
                        insn.getOpcode() == Opcodes.INVOKESTATIC &&
                        insn.owner.equals("net/minecraft/util/GsonHelper") &&
                        insn.name.equals(fromJsonMethod))
                    {
                        methodCallFoundAt0 = i;
                        break;
                    }
                }

                if (methodCallFoundAt0 < 0)
                {
                    ASMAPI.log("ERROR", "Could not find method call to GsonHelper.fromJson");
                    throw new Error("Could not find method call?!");
                }

                ASMAPI.log("INFO", "Found method call to GsonHelper.fromJson at {}", methodCallFoundAt0);

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
                    ASMAPI.log("ERROR", "Could not label above method call?!");
                    throw new Error("Could not find label above?!");
                }

                ASMAPI.log("INFO", "Found label above method call, at {}", labelFoundAt0);

                // Find LabelNode below method call, keep the ASTORE we find before it
                var labelFoundAt1 = -1;
                var astoreVarSlot = -1;
                for(i=methodCallFoundAt0; i < method.instructions.size(); i++)
                {
                    var insn = method.instructions.get(i);
                    if (insn instanceof VarInsnNode)
                    {
                        if (insn.getOpcode() == Opcodes.ASTORE) {
                            astoreVarSlot = insn["var"];
                        }
                    }
                    else if (insn instanceof LabelNode)
                    {
                        labelFoundAt1 = i;
                        break;
                    }
                }

                if (labelFoundAt1 < 0)
                {
                    ASMAPI.log("ERROR", "Could not label below method call?!");
                    throw new Error("Could not find label below?!");
                }

                ASMAPI.log("INFO", "Found label below method call, at {}", labelFoundAt1);

                if (astoreVarSlot < 0)
                {
                    ASMAPI.log("ERROR", "There wasn't an ASTORE before the label?!");
                    throw new Error("Could not find ASTORE?!");
                }

                ASMAPI.log("INFO", "Found ASTORE, variable id = {}", astoreVarSlot);

                // Target label for the IFNONNULL
                var labelEndIf = method.instructions.get(labelFoundAt1);

                ASMAPI.log("INFO", "Injecting code...");

                // Prepare list of instructions to insert
                var list0 = new InsnList();
                addAll(list0,

                    label(),
                        aload(1),
                        aload(6),
                        invokeStatic("dev/gigaherz/jsonmerger/JsonMerger", "combineAllJsonResources",
                            "(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/ResourceLocation;)Lcom/google/gson/JsonElement;", false
                        ),
                        astore(astoreVarSlot),

                    label(),
                        aload(astoreVarSlot),
                        ifNonnull(labelEndIf)
                );

                // Insert instructions at the target location
                var target0 = method.instructions.get(labelFoundAt0);
                method.instructions.insertBefore(target0, list0);

				return classNode;
			}
		}
	}
}
