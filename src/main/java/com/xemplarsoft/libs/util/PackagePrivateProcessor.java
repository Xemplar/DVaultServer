package com.xemplarsoft.libs.util;

import java.util.Set;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("annotation.PackagePrivate")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class PackagePrivateProcessor extends AbstractProcessor {

    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for(Element annotatedElement : roundEnv.getElementsAnnotatedWith(PackagePrivate.class)) {
            if(annotatedElement.getKind() != ElementKind.FIELD
                    && annotatedElement.getKind() != ElementKind.CONSTRUCTOR
                    && annotatedElement.getKind() != ElementKind.METHOD) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        String.format("Fields, Methods and constrcutors can be annotated with @%s",
                                PackagePrivate.class.getSimpleName()),
                        annotatedElement);
                return true;
            } else {
                for(Modifier it : annotatedElement.getModifiers()){
                    if(it.equals(Modifier.PRIVATE) || it.equals(Modifier.PROTECTED) || it.equals(Modifier.PUBLIC)){
                        messager.printMessage(Diagnostic.Kind.ERROR, "Cannot have multiple accessor modifiers", annotatedElement);
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
