package org.autospockgenerate;

import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.RunResult;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.app.Velocity;
import org.autospockgenerate.generate.GenerateFiledMockRegion;
import org.autospockgenerate.generate.GenerateMethodRegion;
import org.autospockgenerate.model.TestInfo;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;


import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.jetbrains.annotations.NotNull;
import org.autospockgenerate.generate.GenerateClassRegion;
import org.autospockgenerate.model.SourceClass;
//import org.jetbrains.plugins.groovy.GroovyLanguage;
//import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
//import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import com.intellij.openapi.vfs.VfsUtil;

public class GenerateTestAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try {
            Project project = e.getData(PlatformCoreDataKeys.PROJECT);
            PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

            PsiJavaFile sourceFile = (PsiJavaFile) psiFile;
            PsiClass[] classes = sourceFile.getClasses();

            // 创建文件
            // 准备数据: 类、被 mock 变量、方法
            String name = psiFile.getName();
            PsiDirectory dic = psiFile.getContainingDirectory();

            PsiClass mainClass = classes[0];
            PsiMethod[] allMethods = mainClass.getAllMethods();

            SourceClass sourceClass = GenerateClassRegion.generateSourceClass(psiFile);
            List<SourceClass> members = GenerateFiledMockRegion.generateMockMembers(mainClass);
            List<TestInfo> testInfos = GenerateMethodRegion.generateAllMethod(psiFile, allMethods, members);

            VelocityEngine ve = buildVelocityEngine();
            // 获取模板文件
            Template t = ve.getTemplate("TestVelocity.vm");
            // 设置变量
            VelocityContext ctx = new VelocityContext();

            StringUtils stringUtils = new StringUtils();
            ctx.put("StringUtils", stringUtils);
            ctx.put("sourceClass", sourceClass);
            ctx.put("mockMembers", members);
            ctx.put("testInfos", testInfos);

            // 输出
            StringWriter sw = new StringWriter();
            t.merge(ctx, sw);
            String text = sw.toString();
            System.out.println(text);

            // 或者直接通过 PsiFileFactory 创建带有内容的 Groovy 文件
            // 创建文件内容
            String testFileName = name.substring(0, name.indexOf(".")) + "Test" + ".groovy";
            PsiFileFactory factory = new PsiFileFactoryImpl(dic.getProject());

            // 或者，如果需要立即保存，可以尝试强制刷新并写入内容
            // 但请注意，这种方式通常不如通过 PsiDocumentManager 来得稳健
            saveGroovyFile(project, psiFile, factory, testFileName, text);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void saveGroovyFile(Project project, PsiFile psiFile, PsiFileFactory factory, String testFileName, String text) {
        // 在事务性操作中保存文件
        new WriteCommandAction.Simple<>(psiFile.getProject()) {
            final PsiDirectory dic = psiFile.getContainingDirectory();
            @Override
            protected void run() throws Throwable {

                VirtualFile virtualFile = dic.getVirtualFile().createChildData(this,  testFileName);
                PsiFile groovyFile = factory.createFileFromText(testFileName, JavaFileType.INSTANCE.getLanguage(), text, true, false);

                Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
                if (document != null) {
                    document.setText(text);
                    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document);
                    PsiDocumentManager.getInstance(project).commitDocument(document);
                }
                // 确保所有关联的 Document 对象被提交
                PsiDocumentManager.getInstance(psiFile.getProject()).commitAllDocuments();
                // 保存所有已修改的文档到磁盘
                FileDocumentManager.getInstance().saveAllDocuments();
            }
        }.execute();
    }

    public VelocityEngine buildVelocityEngine() {
        // 初始化模板引擎
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
        // TODO: 升级使用 classpath 加载
        ve.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        ve.setProperty("file.resource.loader.path", "/Users/t.lan/Work/Space/AutoSpockGenerate/src/main/resources/template"); // 设置模板文件路径
        ve.init();
        return ve;
    }
}
