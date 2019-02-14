
随着一些使用注解生成器（annotationProcessor）的框架的流行，例如[ButterKnife](https://github.com/JakeWharton/butterknife)、[dagger2](https://github.com/google/dagger)、[EventBus 3.0](https://github.com/greenrobot/EventBus)。我需要了解注解生成器的相关知识。

# APT

APT(Annotation Processing Tool)是一种处理注解的工具，它对源代码文件进行检测，找出其中的Annotation。根据注解自动生成代码。Annotation处理器在处理Annotation时可以根据源文件中的Annotation生成额外的源文件和其他的文件（文件具体的内容由Annotation处理器的编写者决定），APT还会编译生成源文件和原来的源文件，将它们一起生成class文件。

**APT工作流程**

1. 定义注解（@XXXX）
2. APT扫描代码中的注解
3. APT依据定义好的注解处理方式进行操作，生成.java文件
4. build工程，生成.class文件

# AnnotationProcessor

AnnotationProcessor是APT工具中的一种，是google开发的内置框架，不需要引入，可以直接在build.gradle文件中使用：

```
dependencies {
     annotationProcessor project(':xx')
     annotationProcessor 'com.jakewharton:butterknife-compiler:8.4.0'
}
```

# AnnotationProcessor和Provided区别

* **AnnotationProcessor**只在编译的时候执行依赖的库，但是库最终不打包到apk中，编译库中的代码没有直接使用的意义，也没有提供开放的api调用，最终的目的是得到编译库中生成的文件，供我们调用。
* **Provided**虽然也是编译时执行，最终不会打包到apk中，但是跟`AnnotationProcessor`有着根本的不同。

```
A、B、C都是Library
A依赖了C，B也依赖了C
App需要同时使用A、B、C
那么其中A（或者B）可以修改与C的依赖关系为Provided
```
由于App使用的aar依赖，所以A、B、C都要编译生成aar，最终会和app一起打包生成apk，但是A、B在编译时又需要依赖C，那么就需要`Provided`了，`Provided`只是确保A、B成功编译生成aar，并不把C编译进A和B。`Provided`起到了避免依赖重复资源的作用。

# 用Kotlin实现一个简单的Processor demo

* 新增一个**Java Library Module**，这里需要注意的是**Java Library** 不是**Android Library**。然后在创建一个AptCreate

```java
@Target(AnnotationTarget.CLASS) // 作用在类上
@Retention(AnnotationRetention.RUNTIME) // 存活时间是运行时
annotation class AptCreate
```

* 再新增一个**Java Library Module**，这里同样是**Java Library**，配置`build.gradle`，下面是代码。

```java
apply plugin: 'java-library'
apply plugin: 'kotlin'

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    compile project(':aptlib')
    compile 'com.squareup:javapoet:1.9.0'
    compile "org.jetbrains.kotlin:kotlin-stdlib:1.2.21"
}

sourceCompatibility = "1.7"
targetCompatibility = "1.7"
```

* 在`aptprocessor Library`中创建AptTestProcess类。

```java
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
```

* 自定义注解类的使用，在在其他Library中添加对`aptlib`和`aptprocess`的依赖，在代码中这样使用

```
@AptCreate
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
```
* `Module`的build.gradle代码如下：

```
apply plugin: 'com.android.application'

android {
    compileSdkVersion 26
    defaultConfig {
        applicationId "com.knight.apt"
        minSdkVersion 21
        targetSdkVersion 26
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation"org.jetbrains.kotlin:kotlin-stdlib-jre7:$kotlin_version"
    implementation 'com.android.support:appcompat-v7:26.1.0'
    implementation 'com.android.support.constraint:constraint-layout:1.0.2'
    testImplementation 'junit:junit:4.12'
    androidTestImplementation 'com.android.support.test:runner:1.0.1'
    androidTestImplementation 'com.android.support.test.espresso:espresso-core:3.0.1'

    //这两行是主要的
    compile project(':aptlib')
    annotationProcessor project(':aptprocess')
}
```

* 编译工程，在`build/generated/source/apt/debug`下就能找到生成的类，如下图：

![image](https://raw.githubusercontent.com/296777513/Picture/master/apt/porject_struct.png)

## 遇到的坑


* 这里遇到第一个坑，因为用的是`Kotlin`，使用`auto-service`一直不生效，所以这里使用了`META-INF/services/javax.annotation.processing.Processor`，并加上：
```
com.knight.aptprocess.AptTestProcess
```

* 遇到的第二个坑，在使用的时候需要使用Java类，不能使用Kotlin来使用自定义注解。否则不会自动生成相应的代码
