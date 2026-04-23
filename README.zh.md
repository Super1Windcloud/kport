# killport

[English](README.md)

一个参考 [`sindresorhus/fkill`](https://github.com/sindresorhus/fkill) 思路实现的 Java 版进程终止工具，既可以作为命令行使用，也可以作为库集成到 Java 项目里。

支持：

- 按 `PID` 终止进程
- 按进程名终止进程
- 按 `:端口` 查找并终止占用端口的进程
- 递归终止子进程树
- 超时后强制终止
- 等待进程退出
- 失败静默处理

## 构建

```bash
mvn package
```

构建完成后产物位于：

```text
target/killport-1.0-SNAPSHOT.jar
```

## 命令行用法

查看帮助：

```bash
java -jar target/killport-1.0-SNAPSHOT.jar --help
```

按 PID 杀进程：

```bash
java -jar target/killport-1.0-SNAPSHOT.jar 1234
```

按端口杀进程：

```bash
java -jar target/killport-1.0-SNAPSHOT.jar :8080
```

按名称杀进程并忽略大小写：

```bash
java -jar target/killport-1.0-SNAPSHOT.jar --ignore-case java
```

超时后强杀并等待退出：

```bash
java -jar target/killport-1.0-SNAPSHOT.jar --force-after-timeout 1000 --wait-for-exit 3000 java
```

可用参数：

- `-f, --force`: 直接强制终止
- `-i, --ignore-case`: 名称匹配忽略大小写
- `-s, --silent`: 忽略单个目标失败
- `-t, --tree`: 同时终止子进程
- `--no-tree`: 仅终止命中的进程
- `--force-after-timeout <ms>`: 先正常结束，超时后强制结束
- `--wait-for-exit <ms>`: 等待进程退出
- `-h, --help`: 输出帮助

## 作为库使用

当前项目本身就是一个标准 Maven `jar`，可以直接作为依赖使用。

### Maven 依赖

如果你把它发布到私有仓库或本地仓库，可以这样引入：

```xml
<dependency>
    <groupId>org.superwindcloud</groupId>
    <artifactId>killport</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

如果只是本地开发测试，可以先安装到本地仓库：

```bash
mvn install
```

### API 示例

```java
import java.time.Duration;
import java.util.List;
import org.superwindcloud.fkill.Fkill;
import org.superwindcloud.fkill.FkillException;

public class Demo {
    public static void main(String[] args) throws FkillException {
        Fkill.Options options = Fkill.Options.builder()
            .ignoreCase(true)
            .tree(true)
            .forceAfterTimeout(Duration.ofMillis(1000))
            .waitForExit(Duration.ofMillis(3000))
            .build();

        Fkill.kill(List.of(":8080", "java"), options);
    }
}
```

完整示例见 `examples/LibraryUsageExample.java`。

## 公开 API

主要入口：

- `Fkill.kill(String input)`
- `Fkill.kill(Collection<String> inputs, Fkill.Options options)`

异常类型：

- `FkillException`

可配置项：

- `force`
- `tree`
- `ignoreCase`
- `silent`
- `forceAfterTimeout`
- `waitForExit`

## 实现说明

- Windows 下按端口查进程使用 `netstat -ano`
- Linux / macOS 下优先尝试 `lsof`，回退到 `ss`
- 名称匹配基于 `ProcessHandle.info().command()`
- 为避免自杀，会跳过当前 Java 进程及其父进程链

## 已知限制

- 进程名匹配依赖 JVM 能否读取到目标进程的命令路径，受操作系统和权限影响
- Unix 系统上如果同时没有 `lsof` 和 `ss`，则无法通过端口解析 PID
- 这个实现尽量贴近 `fkill` 的核心行为，但没有完全复制 Node.js 版本的全部边界处理
