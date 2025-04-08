/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package buildsrc;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import javax.inject.Inject;

import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public abstract class ClassGeneratorTask extends DefaultTask {
    @Inject
    public abstract ProjectLayout getLayout();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    public ClassGeneratorTask() {
        this.getOutputFile().convention(
            this.getLayout()
            .getBuildDirectory()
            .file(this.getName() + "/JavaProbe.class")
        );
    }

    @TaskAction
    public void run() throws IOException {
        Path output = this.getOutputFile().getAsFile().get().toPath();
        byte[] data = buildClass();
        Files.write(output, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static byte[] buildClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(V1_1, ACC_PUBLIC | ACC_SUPER, "JavaProbe", null, "java/lang/Object", null);

        constructor(writer);
        mainMethod(writer);

        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void constructor(ClassVisitor writer) {
        MethodVisitor mtd = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mtd.visitCode();
        mtd.visitVarInsn(ALOAD, 0);
        mtd.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mtd.visitInsn(RETURN);
        mtd.visitMaxs(1, 1);
        mtd.visitEnd();
    }

    private static final String[] PROPERTIES = {
        "java.home",
        "java.version",
        "java.vendor",
        "java.runtime.name",
        "java.runtime.version",
        "java.vm.name",
        "java.vm.version",
        "java.vm.vendor",
        "os.arch"
    };

    private static void mainMethod(ClassVisitor classWriter) {
        MethodVisitor mtd = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mtd.visitCode();

        for (String prop : PROPERTIES) {
            String prefix = "JAVA_PROBE: " + prop + " ";
            // System.out.print(prefix);
            mtd.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mtd.visitLdcInsn(prefix);
            mtd.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V", false);

            // System.out.println(System.getProperty(prop, "unset"));
            mtd.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mtd.visitLdcInsn(prop);
            mtd.visitLdcInsn("unset");
            mtd.visitMethodInsn(INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false);
            mtd.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }

        mtd.visitInsn(RETURN);

        mtd.visitMaxs(3, 2);

        mtd.visitEnd();
    }
}
