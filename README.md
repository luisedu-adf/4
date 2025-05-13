- mudou-se a estrutura de pastas para o padrão maven
- optou-se por usar java-8-corretto para compilar
- jars não encontrados no maven upados para o github packages
- usou-se github actions para testar a build remota

1. forked `https://github.com/futurepages/4`
2. cloned
3. criou-se o pom inicial

**pom.xml**
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>org.work7.futurepages</groupId>
    <artifactId>futurepages</artifactId>
    <version>4.0.4</version>
    <packaging>jar</packaging>

    <repositories>
        <repository>
            <id>github</id>
            <name>GitHub Packages</name>
            <url>https://maven.pkg.github.com/luisedu-adf/4</url>
        </repository>
    </repositories>

    <properties>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <maven.compiler.encoding>UTF-8</maven.compiler.encoding>
    </properties>


    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <dependencies>
			...
	</dependencies>
</project>   
```

4. Procurou-se GAVs da pasta lib/

na raiz do projeto:
```bash
mkdir RESOLVE_JARS

cp lib/*.jar lib/jai-1_1_3/*.jar lib/jee/*.jar lib/jasper_reports/*.jar lib/jersey/*.jar lib/hibernate-4.1.7.Final/required/*.jar lib/okhttp/*.jar lib/pdfbox-lib/*.jar lib/test/*.jar RESOLVE_JARS/

python jar-to-gav.py RESOLVE_JARS/

```

- Usou-se um script em python (`jar-to-gav.py`) para procurar as dependências necessárias na central do Maven, usando o checksum dos jars, com isso a maioria foi encontrada.
- Por hora optou-se por upar todos os que não foram encontrados pro github packages na intenção de manter a integridade do projeto
- Os arquivos não encontrados foram upados pro github usando um script em bash (`upload-to-github-packages.sh`)

jars não encontrados pelo checksum (18) :
```
asm-attrs.jar 
asm.jar 
javassist-3.23.1-GA.jar 
jmxri.jar
mirror-1.5.1.jar
itext-2.1.7.js6.jar 
mail.jar
minium.jar
json-lib-0.8.jar
bcmail-jdk14-132.jar 
jgroups-all.jar
ezmorph-0.8.1.jar 
json.jar
activation.jar 
bcprov-jdk16-137.jar 
flyingsaucerproject.jar 
jai_codec.jar 
jai_core.jar 
```

5. refatorou-se [[ImageUtil.java]], que estava dependendo da jdk 1.8.0_202 ( é preciso testar se a refatoração funcionou )


6. ao tentar compilar pela primeira vez com o maven, tive esse problema com o itext
	[ERROR] Failed to read artifact descriptor for com.lowagie:itext:jar:2.1.7.js2
	
a solução foi reupar o itext no github package usando GAVs especifícos:
```xml
<dependency>
  <groupId>com.lowagie</groupId>
  <artifactId>itext</artifactId>
  <version>2.1.7.js2</version>
</dependency>
```
para todos os outros que foram upados no github packages, segui o seguinte padrão de GAV : `com.work7.futurepages:<nome-do-arquivo>:1.0.0`


7.  executou-se o mvn pra compilar e gerar o jar: 
```
mvn clean package
```


