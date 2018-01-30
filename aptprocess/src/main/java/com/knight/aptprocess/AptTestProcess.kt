package com.knight.aptprocess

import com.knight.aptlib.AptCreate
import com.squareup.javapoet.MethodSpec
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import java.io.IOException


/**
 * 自定义注解类
 *
 * @author liyachao
 * @date 2018/1/29
 */

class AptTestProcess : AbstractProcessor() {

    override fun getSupportedAnnotationTypes(): Set<String> {
        val types = LinkedHashSet<String>()
        types.add(AptCreate::class.java.canonicalName)
        return types
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        //TODO("not implemented")
        // To change body of created functions use File | Settings | File Templates.
        System.out.println("======>start")

        /**
         * 定义了方法
         */
        val main = MethodSpec.methodBuilder("printHello")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(Void.TYPE)
                .addParameter(Array<String>::class.java, "args")
                .addStatement("\$T.out.println(\$S)", System::class.java, "Hello, Kotlin!")
                .build()

        /**
         * 定义类
         */
        val helloWorld = TypeSpec.classBuilder("HelloWorld")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(main)
                .build()

        /**
         * 定义包名
         */
        val javaFile = JavaFile.builder("com.knight.apt", helloWorld)
                .build()

        try {
            javaFile.writeTo(processingEnv.filer)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        System.out.println("=======>end")
        return false
    }
}