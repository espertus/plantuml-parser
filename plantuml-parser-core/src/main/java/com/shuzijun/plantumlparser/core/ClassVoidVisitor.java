package com.shuzijun.plantumlparser.core;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * 类
 *
 * @author shuzijun
 */
public class ClassVoidVisitor extends VoidVisitorAdapter<PUml> {

    private final String packageName;

    private final ParserConfig parserConfig;

    public ClassVoidVisitor(String packageName, ParserConfig parserConfig) {
        this.packageName = packageName;
        this.parserConfig = parserConfig;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration cORid, PUml pUml) {
        if (!(pUml instanceof PUmlView)) {
            super.visit(cORid, pUml);
            return;
        }
        PUmlView pUmlView = (PUmlView) pUml;
        PUmlClass pUmlClass = createUmlClass();

        pUmlClass.setClassName(cORid.getNameAsString());
        if (cORid.isInterface()) {
            pUmlClass.setClassType("interface");
        } else {
            pUmlClass.setClassType("class");
            for (Modifier modifier : cORid.getModifiers()) {
                if (modifier.toString().trim().contains("abstract")) {
                    pUmlClass.setClassType("abstract class");
                    break;
                }
            }
        }
        cORid.getFields().forEach(p -> p.accept(this, pUmlClass));
        cORid.getConstructors().forEach(p -> p.accept(this, pUmlClass));
        cORid.getMethods().forEach(p -> p.accept(this, pUmlClass));

        pUmlView.addPUmlClass(pUmlClass);

        Node node = cORid.getParentNode().get();

        NodeList<ImportDeclaration> importDeclarations = parseImport(node, pUmlClass, pUmlView);

        Map<String, String> importMap = new HashMap<>();
        if (importDeclarations != null) {
            for (ImportDeclaration importDeclaration : importDeclarations) {
                importMap.put(importDeclaration.getName().getIdentifier(), importDeclaration.getName().toString());
            }
        }
        if (cORid.getImplementedTypes().size() != 0) {
            for (ClassOrInterfaceType implementedType : cORid.getImplementedTypes()) {
                PUmlRelation pUmlRelation = new PUmlRelation();
                pUmlRelation.setTarget(getPackageNamePrefix(pUmlClass.getPackageName()) + pUmlClass.getClassName());
                if (importMap.containsKey(implementedType.getNameAsString())) {
                    if (parserConfig.isShowPackage()) {
                        pUmlRelation.setSource(importMap.get(implementedType.getNameAsString()));
                    } else {
                        pUmlRelation.setSource(implementedType.getNameAsString());
                    }
                } else {
                    pUmlRelation.setSource(getPackageNamePrefix(pUmlClass.getPackageName()) + implementedType.getNameAsString());
                }
                pUmlRelation.setRelation("<|..");
                pUmlView.addPUmlRelation(pUmlRelation);
            }
        }

        if (cORid.getExtendedTypes().size() != 0) {
            for (ClassOrInterfaceType extendedType : cORid.getExtendedTypes()) {
                PUmlRelation pUmlRelation = new PUmlRelation();
                pUmlRelation.setTarget(getPackageNamePrefix(pUmlClass.getPackageName()) + pUmlClass.getClassName());
                if (importMap.containsKey(extendedType.getNameAsString())) {
                    if (parserConfig.isShowPackage()) {
                        pUmlRelation.setSource(importMap.get(extendedType.getNameAsString()));
                    } else {
                        pUmlRelation.setSource(extendedType.getNameAsString());
                    }
                } else {
                    pUmlRelation.setSource(getPackageNamePrefix(pUmlClass.getPackageName()) + extendedType.getNameAsString());
                }
                pUmlRelation.setRelation("<|--");
                pUmlView.addPUmlRelation(pUmlRelation);

            }
        }
        super.visit(cORid, pUml);
    }

    @Override
    public void visit(EnumDeclaration enumDeclaration, PUml pUml) {
        if (!(pUml instanceof PUmlView)) {
            super.visit(enumDeclaration, pUml);
            return;
        }
        PUmlView pUmlView = (PUmlView) pUml;
        PUmlClass pUmlClass = createUmlClass();

        pUmlClass.setClassName(enumDeclaration.getNameAsString());
        pUmlClass.setClassType("enum");

        enumDeclaration.getEntries().forEach(p -> p.accept(this, pUmlClass));
        enumDeclaration.getFields().forEach(p -> p.accept(this, pUmlClass));
        enumDeclaration.getConstructors().forEach(p -> p.accept(this, pUmlClass));
        enumDeclaration.getMethods().forEach(p -> p.accept(this, pUmlClass));

        pUmlView.addPUmlClass(pUmlClass);
        super.visit(enumDeclaration, pUml);
    }

    @Override
    public void visit(FieldDeclaration field, PUml pUml) {
        if (!(pUml instanceof PUmlClass)) {
            super.visit(field, pUml);
            return;
        }
        PUmlClass pUmlClass = (PUmlClass) pUml;
        PUmlField pUmlField = new PUmlField();
        if (field.getModifiers().size() != 0) {
            for (Modifier modifier : field.getModifiers()) {
                if (VisibilityUtils.isVisibility(modifier.toString().trim())) {
                    pUmlField.setVisibility(modifier.toString().trim());
                    break;
                }
            }
        }
        if (parserConfig.isFieldModifier(pUmlField.getVisibility())) {
            pUmlField.setStatic(field.isStatic());
            pUmlField.setType(field.getVariables().getFirst().get().getTypeAsString());
            pUmlField.setName(field.getVariables().getFirst().get().getNameAsString());
            pUmlClass.addPUmlFieldList(pUmlField);
        }
    }


    @Override
    public void visit(ConstructorDeclaration constructor, PUml pUml) {
        if (!(pUml instanceof PUmlClass)) {
            super.visit(constructor, pUml);
            return;
        }
        if (!parserConfig.isShowConstructors()) {
            return;
        }
        PUmlClass pUmlClass = (PUmlClass) pUml;
        PUmlMethod pUmlMethod = new PUmlMethod();
        if (constructor.getModifiers().size() != 0) {
            for (Modifier modifier : constructor.getModifiers()) {
                if (VisibilityUtils.isVisibility(modifier.toString().trim())) {
                    pUmlMethod.setVisibility(modifier.toString().trim());
                    break;
                }
            }
        }
        if (parserConfig.isMethodModifier(pUmlMethod.getVisibility())) {
            pUmlMethod.setStatic(constructor.isStatic());
            pUmlMethod.setAbstract(constructor.isAbstract());
            pUmlMethod.setReturnType("<<Create>>");
            pUmlMethod.setName(constructor.getNameAsString());
            for (Parameter parameter : constructor.getParameters()) {
                pUmlMethod.addParam(parameter.getTypeAsString());
            }
            pUmlClass.addPUmlMethodList(pUmlMethod);
        }
    }

    @Override
    public void visit(MethodDeclaration method, PUml pUml) {
        if (!(pUml instanceof PUmlClass)) {
            super.visit(method, pUml);
            return;
        }
        PUmlClass pUmlClass = (PUmlClass) pUml;

        PUmlMethod pUmlMethod = new PUmlMethod();

        if (method.getModifiers().size() != 0) {
            for (Modifier modifier : method.getModifiers()) {
                if (VisibilityUtils.isVisibility(modifier.toString().trim())) {
                    pUmlMethod.setVisibility(modifier.toString().trim());
                    break;
                }
            }
        }
        if (parserConfig.isMethodModifier(pUmlMethod.getVisibility())) {
            pUmlMethod.setStatic(method.isStatic());
            pUmlMethod.setAbstract(method.isAbstract());
            pUmlMethod.setReturnType(method.getTypeAsString());
            pUmlMethod.setName(method.getNameAsString());
            for (Parameter parameter : method.getParameters()) {
                pUmlMethod.addParam(parameter.getTypeAsString());
            }
            pUmlClass.addPUmlMethodList(pUmlMethod);
        }
    }

    @Override
    public void visit(EnumConstantDeclaration enumConstantDeclaration, PUml pUml) {
        if (!(pUml instanceof PUmlClass)) {
            super.visit(enumConstantDeclaration, pUml);
            return;
        }
        PUmlClass pUmlClass = (PUmlClass) pUml;
        PUmlField pUmlField = new PUmlField();

        pUmlField.setName(enumConstantDeclaration.getNameAsString());
        pUmlField.setType("");
        pUmlField.setVisibility("public");
        pUmlClass.addPUmlFieldList(pUmlField);

    }

    private PUmlClass createUmlClass() {
        PUmlClass pUmlClass = new PUmlClass();
        if (parserConfig.isShowPackage()) {
            pUmlClass.setPackageName(packageName);
        } else {
            pUmlClass.setPackageName("");
        }
        return pUmlClass;
    }

    private NodeList<ImportDeclaration> parseImport(Node node, PUmlClass pUmlClass, PUmlView pUmlView) {
        if (node instanceof CompilationUnit) {
            return ((CompilationUnit) node).getImports();
        } else if (node instanceof ClassOrInterfaceDeclaration) {
            pUmlClass.setClassName(((ClassOrInterfaceDeclaration) node).getNameAsString() + "." + pUmlClass.getClassName());

            Node parentNode = node.getParentNode().get();
            if (parentNode instanceof CompilationUnit) {
                PUmlRelation pUmlRelation = new PUmlRelation();
                pUmlRelation.setTarget(getPackageNamePrefix(pUmlClass.getPackageName()) + pUmlClass.getClassName());
                pUmlRelation.setSource(getPackageNamePrefix(pUmlClass.getPackageName()) + pUmlClass.getClassName().substring(0, pUmlClass.getClassName().lastIndexOf(".")));
                pUmlRelation.setRelation("+..");
                pUmlView.addPUmlRelation(pUmlRelation);
            }
            parseImport(parentNode, pUmlClass, pUmlView);
        }
        return null;
    }

    private String getPackageNamePrefix(String packageName) {
        if (packageName == null || packageName.trim().equals("")) {
            return "";
        } else {
            return packageName + ".";
        }
    }
}
