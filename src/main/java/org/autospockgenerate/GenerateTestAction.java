package org.autospockgenerate;

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

import java.io.StringWriter;
import java.util.List;
import java.util.Properties;

public class GenerateTestAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try {
            Project project = e.getData(PlatformCoreDataKeys.PROJECT);
            PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
//            String classPath = psiFile.getVirtualFile().getPath();
//            String title = "Hello World!";
//            Messages.showMessageDialog(project, classPath, title, Messages.getInformationIcon());

            PsiJavaFile javaFile = (PsiJavaFile) psiFile;
            PsiClass[] classes = javaFile.getClasses();

            // 创建文件
            // 准备数据: 类、被 mock 变量、方法
            String name = psiFile.getName();

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
            ctx.put("sourceClass", sourceClass);
            ctx.put("mockMembers", members);
            ctx.put("methods", testInfos);

            // 输出
            StringWriter sw = new StringWriter();
            t.merge(ctx, sw);
            System.out.println(sw);
//        PsiFile testFile = PsiFileFactory.getInstance(project).createFileFromText(sourceClass.name+ "Test.groovy", var5, var3);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
