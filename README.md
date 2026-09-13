# Chain-store website foundation

Spring Boot 4.1.1, Java 26, Maven Wrapper 3.9.16, executable WAR.
Root package: `com.example.demo`. Business features are intentionally deferred.

## Build on Windows

Maven uses `JAVA_HOME`, which may differ from `java` on `PATH`.
Point it at an installed **JDK 26** in the current PowerShell session:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26.0.2.1' # Adjust to your installation.
.\mvnw.cmd -version
.\mvnw.cmd clean compile
.\mvnw.cmd test
.\mvnw.cmd clean test
.\mvnw.cmd clean package
```

The WAR is written to `target/demo-0.0.1-SNAPSHOT.war`.
The wrapper handles a Maven home directory with no PowerShell `Target` property.

To keep build downloads and temporary files inside this workspace:

```powershell
New-Item -ItemType Directory -Force .build-tools/tmp | Out-Null
$env:MAVEN_USER_HOME = "$PWD\.build-tools"
$env:TEMP = "$PWD\.build-tools\tmp"
$env:TMP = $env:TEMP
.\mvnw.cmd "-Dmaven.repo.local=$PWD\.build-tools\repository" "-Djava.io.tmpdir=$env:TEMP" clean test
```

`.build-tools/` and `target/` are ignored by Git.

## Runtime configuration

Set these environment variables in the process that starts the application.
Do not store real values in Git. `.env.example` lists the variable names;
Spring Boot does **not** automatically load `.env` files.

| Variable | Use |
| --- | --- |
| `DB_URL` | Required SQL Server JDBC URL, including server, port and database name |
| `DB_USERNAME` | Required database username |
| `DB_PASSWORD` | Required database password |
| `JWT_SECRET` | Reserved for future JWT integration |
| `CLOUDINARY_CLOUD_NAME` | Reserved for future Cloudinary integration |
| `CLOUDINARY_API_KEY` | Reserved for future Cloudinary integration |
| `CLOUDINARY_API_SECRET` | Reserved for future Cloudinary integration |

URL shape: `jdbc:sqlserver://<host>:1433;databaseName=<database>;encrypt=true;trustServerCertificate=false`.
Use a server certificate trusted by the JVM. Create/configure the database separately.
Hibernate DDL and SQL script initialization are disabled; startup does not create,
update or delete database tables.

After supplying the database environment variables:

```powershell
.\mvnw.cmd spring-boot:run
# Or, after packaging:
java -jar target/demo-0.0.1-SNAPSHOT.war
```

Use Ctrl+C to stop the application. For workspace-local temporary files when
using the Maven option above, also pass
`"-Dspring-boot.run.jvmArguments=-Djava.io.tmpdir=$env:TEMP"` to `spring-boot:run`.
Missing SQL Server configuration is an external environment blocker.
**BUILD SUCCESS does not mean DATABASE CONNECTION SUCCESS.**

## Foundation boundaries and verification

- Packages: `config`, `controller`, `dto`, `entity`, `repository`, `service`,
  `security`, `websocket`, `exception`, `util`; `package-info.java` records their purpose.
- JSP views belong in `src/main/webapp/WEB-INF/views/`, resolved with prefix
  `/WEB-INF/views/` and suffix `.jsp`. Jakarta JSTL API and implementation are included.
  No page or controller is implemented yet; the resolver test does not compile/render a JSP.
- Spring Security keeps its default protection. No custom login, JWT filter,
  user store or authentication implementation is provided.
- WebSocket, JWT, SiteMesh and Cloudinary dependencies are available for later work.
  No messaging endpoint, decorator mapping or external Cloudinary call is configured.
- Tests retain the application context test and verify JPA startup, a real
  `SELECT 1` against an isolated in-memory H2 database, and the JSP view path.
  H2 has **test scope only** and `application-test.properties` exists only under
  `src/test/resources`; it is not a SQL Server emulator or a runtime fallback.
- Mockito is loaded explicitly as a test JVM agent for modern JDKs. The JVM may
  still emit an informational class-data-sharing warning.

## Dependency choices

Spring Boot manages the versions of Spring starters, SQL Server JDBC, Jasper,
Jakarta JSTL and H2. JJWT uses `jjwt-api`, runtime `jjwt-impl` and runtime
`jjwt-jackson` at 0.13.0. Its Jackson 2 runtime is separate from Boot 4's Jackson 3
web stack; both versions are managed by Spring Boot. Cloudinary uses `cloudinary-http5` 2.4.0.

SiteMesh is pinned to **3.3.0-RC1**, a pre-release explicitly targeting Spring Boot 4,
Tomcat 11 and Servlet 6.1. Review the stable 3.3 release before implementing layouts.
Only the core dependency is included at this stage.

References: [Spring Boot requirements](https://docs.spring.io/spring-boot/system-requirements.html),
[JSP support](https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.embedded-container.jsp-limitations),
[JJWT installation](https://github.com/jwtk/jjwt#installation),
[SiteMesh release notes](https://github.com/sitemesh/sitemesh3/releases/tag/3.3.0-RC1),
[Cloudinary Java SDK](https://cloudinary.com/documentation/java_integration).
