# H2T men's shirt store chain

Chạy cả web và SQL Server bằng Docker Desktop: xem [README-Docker.md](README-Docker.md).

Spring Boot 4.1.1, Java 26, Maven Wrapper 3.9.16, executable WAR.
Root package: `com.example.demo`. The phase-2.5 multi-shop domain, JPA entities and repositories
are available; business services, controllers and frontend are deferred.
See [database design](docs/database-design.md) for relationships, constraints and ownership.

## Build on Windows

Install **JDK 26**. Maven Toolchains automatically selects an installed JDK 26
for compilation and tests, even if Maven itself starts under another JDK through
`JAVA_HOME`. No machine-specific JDK path is stored in the POM.

```powershell
.\mvnw.cmd -version
.\mvnw.cmd clean compile
.\mvnw.cmd test
.\mvnw.cmd clean test
.\mvnw.cmd clean package
```

`mvnw.cmd -version` reports the JDK running Maven; the build's
`Selected JDK toolchain` / `Toolchain in maven-compiler-plugin` messages show the
JDK used to compile and test. If a nonstandard JDK installation is not discovered,
set `JAVA26_HOME` to its location. The build fails early if no JDK 26 is available.
See [Maven JDK discovery](https://maven.apache.org/plugins/maven-toolchains-plugin/toolchains/jdk-discovery.html).

For direct `java -jar` execution, the `java` command must still use JDK 26.

The WAR is written to `target/demo-0.0.1-SNAPSHOT.war`.
The wrapper handles a Maven home directory with no PowerShell `Target` property.

To keep build downloads and temporary files inside this workspace:

```powershell
New-Item -ItemType Directory -Force .build-tools/tmp | Out-Null
$env:MAVEN_USER_HOME = "$PWD\.build-tools"
$env:TEMP = "$PWD\.build-tools\tmp"
$env:TMP = $env:TEMP
.\mvnw.cmd "-Dmaven.repo.local=$PWD\.build-tools\repository" "-Djava.io.tmpdir=$env:TEMP" "-Duser.home=$PWD\.build-tools\home" clean test
```

The local `user.home` override also keeps the JDK discovery cache inside the workspace.

`.build-tools/` and `target/` are ignored by Git.

## Runtime configuration

Set these environment variables in the process that starts the application,
or define the same `DB_*` keys in `application-local.properties` at the project root.
That optional local file is loaded automatically, ignored by Git and excluded from
the WAR. Environment variables take precedence over its values.
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
The base configuration keeps Hibernate DDL disabled. For local development,
the ignored `application-local.properties` activates `spring.profiles.active=dev`.
The tracked `application-dev.properties` uses `ddl-auto=update` so Hibernate creates
missing tables and updates mappings in the configured database without dropping it.
Schema errors fail startup. SQL script initialization remains disabled.

For the configured local instance, `application-local.properties` contains the
URL for `localhost:1433/H2T_DB` and login `h2t_user`. Supply `DB_PASSWORD`
locally before running. The supplied URL uses `trustServerCertificate=true`
for this local setup.

When upgrading the existing phase-2 schema, first read the
[migration requirements](docs/database-design.md#safe-local-upgrade-from-phase-2)
and run [the guarded local migration](docs/sql/phase-2.5-local-pre-update.sql)
in H2T_DB with older application instances stopped. It handles legacy NOT NULL
order columns and the old role CHECK that Hibernate update does not replace.
The script intentionally rejects populated legacy data needing a business backfill.
The migration has already been applied to this local database.

After supplying the database credentials:

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
- Spring Security keeps its default protection. No custom login, JWT filter
  or authentication implementation is provided.
- WebSocket, JWT, SiteMesh and Cloudinary dependencies are available for later work.
  No messaging endpoint, decorator mapping or external Cloudinary call is configured.
- Tests retain the application context test, verify the JSP view path and exercise
  persistence, uniqueness, quantity checks, lazy associations and order snapshots
  using H2, including multi-shop orders, addresses, history and shipper assignment.
  The test profile uses `ddl-auto=update` on a fresh in-memory database.
  H2 has **test scope only** and `application-test.properties` exists only under
  `src/test/resources`; it is not a SQL Server emulator or a runtime fallback.
- A separate test generates SQL Server DDL into `target/sqlserver-schema.sql`
  for review without opening a database connection. Local `dev` startup applies
  Hibernate schema updates to SQL Server; other profiles retain the base `none` setting.
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
