# Admin 后端管理 API 完善实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 补齐 DayDayUP admin 后端管理 API，并沉淀分页、DTO、Service CRUD 抽象和通配符权限能力。

**架构：** Controller 保持显式 API 契约，不做 BaseController。公共分页与 DTO 父类放入 `daydayup-common-core`，CRUD Service 模板放入 `daydayup-common-mybatis`，admin 各管理域通过具体 Service 继承模板并覆盖校验、转换和查询条件。安全层增加通配符权限匹配，支持 `admin:*` 覆盖 `admin:` 前缀权限。

**技术栈：** Java 21、Spring Boot 3.5、Spring Security Method Security、MyBatis-Plus、Jakarta Validation、Lombok、JUnit 5。

---

## 文件结构

### 公共模块

- 创建：`daydayup-common/daydayup-common-core/src/main/java/com/yuan/daydayup/common/core/dto/BaseRequestDTO.java`  
  写操作 DTO 父类，暂不放业务字段。
- 创建：`daydayup-common/daydayup-common-core/src/main/java/com/yuan/daydayup/common/core/dto/BasePageQueryDTO.java`  
  分页查询 DTO 父类，封装 `pageNum/pageSize`。
- 创建：`daydayup-common/daydayup-common-core/src/main/java/com/yuan/daydayup/common/core/dto/BaseStatusDTO.java`  
  状态变更 DTO 父类。
- 创建：`daydayup-common/daydayup-common-core/src/main/java/com/yuan/daydayup/common/core/page/PageResult.java`  
  统一分页响应。
- 修改：`daydayup-common/daydayup-common-core/pom.xml`  
  增加测试依赖。
- 创建：`daydayup-common/daydayup-common-core/src/test/java/com/yuan/daydayup/common/core/dto/BasePageQueryDTOTest.java`  
  验证分页 DTO 默认值与边界。
- 创建：`daydayup-common/daydayup-common-core/src/test/java/com/yuan/daydayup/common/core/page/PageResultTest.java`  
  验证分页结果构造。
- 创建：`daydayup-common/daydayup-common-mybatis/src/main/java/com/yuan/daydayup/common/mybatis/service/BaseCrudService.java`  
  CRUD Service 基础接口。
- 创建：`daydayup-common/daydayup-common-mybatis/src/main/java/com/yuan/daydayup/common/mybatis/service/DeletableCrudService.java`  
  可删除能力接口。
- 创建：`daydayup-common/daydayup-common-mybatis/src/main/java/com/yuan/daydayup/common/mybatis/service/AbstractCrudService.java`  
  CRUD 模板方法实现。
- 修改：`daydayup-common/daydayup-common-mybatis/pom.xml`  
  增加测试依赖。
- 创建：`daydayup-common/daydayup-common-mybatis/src/test/java/com/yuan/daydayup/common/mybatis/service/AbstractCrudServiceTest.java`  
  验证模板方法调用顺序与可选删除边界。
- 创建：`daydayup-common/daydayup-common-security/src/main/java/com/yuan/daydayup/common/security/permission/WildcardPermissionEvaluator.java`  
  通配符权限匹配工具。
- 创建：`daydayup-common/daydayup-common-security/src/main/java/com/yuan/daydayup/common/security/config/MethodSecurityConfig.java`  
  注册 `hasAuthority` 使用的 `PermissionEvaluator` 或表达式处理器。
- 修改：`daydayup-common/daydayup-common-security/pom.xml`  
  增加测试依赖。
- 创建：`daydayup-common/daydayup-common-security/src/test/java/com/yuan/daydayup/common/security/permission/WildcardPermissionEvaluatorTest.java`  
  验证 `admin:*` 匹配 `admin:user:list`。

### Admin 模块

- 修改：`daydayup-platform/daydayup-admin/daydayup-admin-biz/pom.xml`  
  增加测试依赖。
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/*`  
  用户、角色、权限、菜单、字典、字典项、操作日志 DTO。
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/vo/*`  
  各管理域 VO。
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/service/*Service.java`  
  各管理域 Service 接口。
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/service/impl/*ServiceImpl.java`  
  各管理域 Service 实现。
- 修改：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/controller/UserController.java`  
  保留 Feign 契约，新增用户管理 API 方法或拆出管理 Controller。
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/controller/RoleController.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/controller/PermissionController.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/controller/MenuController.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/controller/DictController.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/controller/DictItemController.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/controller/OperLogController.java`
- 修改：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/resources/db/init.sql`  
  补齐细粒度权限种子数据，超级管理员仍只绑定 `admin:*`。
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/test/java/com/yuan/daydayup/admin/service/*ServiceImplTest.java`  
  各管理域业务规则单元测试。
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/test/java/com/yuan/daydayup/admin/controller/*ControllerTest.java`  
  各管理域 Controller 路由和权限注解测试。

---

## 任务 1：公共分页 DTO 与分页响应

**文件：**
- 修改：`daydayup-common/daydayup-common-core/pom.xml`
- 创建：`daydayup-common/daydayup-common-core/src/main/java/com/yuan/daydayup/common/core/dto/BaseRequestDTO.java`
- 创建：`daydayup-common/daydayup-common-core/src/main/java/com/yuan/daydayup/common/core/dto/BasePageQueryDTO.java`
- 创建：`daydayup-common/daydayup-common-core/src/main/java/com/yuan/daydayup/common/core/dto/BaseStatusDTO.java`
- 创建：`daydayup-common/daydayup-common-core/src/main/java/com/yuan/daydayup/common/core/page/PageResult.java`
- 创建：`daydayup-common/daydayup-common-core/src/test/java/com/yuan/daydayup/common/core/dto/BasePageQueryDTOTest.java`
- 创建：`daydayup-common/daydayup-common-core/src/test/java/com/yuan/daydayup/common/core/page/PageResultTest.java`

- [ ] **步骤 1：增加 common-core 测试依赖**

在 `daydayup-common/daydayup-common-core/pom.xml` 的 `<dependencies>` 内追加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **步骤 2：编写分页 DTO 失败测试**

创建 `BasePageQueryDTOTest.java`：

```java
package com.yuan.daydayup.common.core.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BasePageQueryDTOTest {

    @Test
    void shouldUseDefaultPageValues() {
        BasePageQueryDTO query = new BasePageQueryDTO();

        assertThat(query.getPageNum()).isEqualTo(1L);
        assertThat(query.getPageSize()).isEqualTo(10L);
    }

    @Test
    void shouldNormalizeInvalidPageValues() {
        BasePageQueryDTO query = new BasePageQueryDTO();
        query.setPageNum(0L);
        query.setPageSize(200L);

        assertThat(query.normalizedPageNum()).isEqualTo(1L);
        assertThat(query.normalizedPageSize()).isEqualTo(100L);
    }
}
```

- [ ] **步骤 3：编写分页结果失败测试**

创建 `PageResultTest.java`：

```java
package com.yuan.daydayup.common.core.page;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultTest {

    @Test
    void shouldCreatePageResult() {
        PageResult<String> result = PageResult.of(List.of("a", "b"), 12L, 2L, 5L);

        assertThat(result.getRecords()).containsExactly("a", "b");
        assertThat(result.getTotal()).isEqualTo(12L);
        assertThat(result.getPageNum()).isEqualTo(2L);
        assertThat(result.getPageSize()).isEqualTo(5L);
        assertThat(result.getPages()).isEqualTo(3L);
    }
}
```

- [ ] **步骤 4：运行测试验证失败**

运行：

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-common/daydayup-common-core test
```

预期：编译失败，提示 `BasePageQueryDTO` 和 `PageResult` 不存在。

- [ ] **步骤 5：实现基础请求 DTO**

创建 `BaseRequestDTO.java`：

```java
package com.yuan.daydayup.common.core.dto;

import java.io.Serial;
import java.io.Serializable;

public class BaseRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
```

- [ ] **步骤 6：实现分页查询 DTO**

创建 `BasePageQueryDTO.java`：

```java
package com.yuan.daydayup.common.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BasePageQueryDTO extends BaseRequestDTO {

    @Min(1)
    private Long pageNum = 1L;

    @Min(1)
    @Max(100)
    private Long pageSize = 10L;

    public Long normalizedPageNum() {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    public Long normalizedPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 100L);
    }
}
```

- [ ] **步骤 7：实现状态 DTO**

创建 `BaseStatusDTO.java`：

```java
package com.yuan.daydayup.common.core.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BaseStatusDTO extends BaseRequestDTO {

    @NotNull
    private Integer status;
}
```

- [ ] **步骤 8：实现分页响应对象**

创建 `PageResult.java`：

```java
package com.yuan.daydayup.common.core.page;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<T> records;
    private Long total;
    private Long pageNum;
    private Long pageSize;
    private Long pages;

    public static <T> PageResult<T> of(List<T> records, Long total, Long pageNum, Long pageSize) {
        long safeTotal = total == null ? 0L : total;
        long safePageNum = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long safePageSize = pageSize == null || pageSize < 1 ? 10L : pageSize;
        long pages = safeTotal == 0 ? 0L : (safeTotal + safePageSize - 1) / safePageSize;
        return new PageResult<>(records, safeTotal, safePageNum, safePageSize, pages);
    }
}
```

- [ ] **步骤 9：运行测试验证通过**

运行：

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-common/daydayup-common-core test
```

预期：`BasePageQueryDTOTest` 和 `PageResultTest` 通过。

- [ ] **步骤 10：Commit**

```bash
git add daydayup-common/daydayup-common-core/pom.xml \
  daydayup-common/daydayup-common-core/src/main/java/com/yuan/daydayup/common/core/dto \
  daydayup-common/daydayup-common-core/src/main/java/com/yuan/daydayup/common/core/page \
  daydayup-common/daydayup-common-core/src/test/java/com/yuan/daydayup/common/core
git commit -m "feat(common): add base dto and page result"
```

---

## 任务 2：CRUD Service 抽象

**文件：**
- 修改：`daydayup-common/daydayup-common-mybatis/pom.xml`
- 创建：`daydayup-common/daydayup-common-mybatis/src/main/java/com/yuan/daydayup/common/mybatis/service/BaseCrudService.java`
- 创建：`daydayup-common/daydayup-common-mybatis/src/main/java/com/yuan/daydayup/common/mybatis/service/DeletableCrudService.java`
- 创建：`daydayup-common/daydayup-common-mybatis/src/main/java/com/yuan/daydayup/common/mybatis/service/AbstractCrudService.java`
- 创建：`daydayup-common/daydayup-common-mybatis/src/test/java/com/yuan/daydayup/common/mybatis/service/AbstractCrudServiceTest.java`

- [ ] **步骤 1：增加 common-mybatis 测试依赖**

在 `daydayup-common/daydayup-common-mybatis/pom.xml` 的 `<dependencies>` 内追加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **步骤 2：编写 CRUD 抽象失败测试**

创建 `AbstractCrudServiceTest.java`：

```java
package com.yuan.daydayup.common.mybatis.service;

import com.yuan.daydayup.common.core.dto.BasePageQueryDTO;
import com.yuan.daydayup.common.core.dto.BaseStatusDTO;
import com.yuan.daydayup.common.core.page.PageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractCrudServiceTest {

    @Test
    void shouldExposeCrudContract() {
        TestService service = new TestService();
        BasePageQueryDTO query = new BasePageQueryDTO();
        BaseStatusDTO status = new BaseStatusDTO();
        status.setStatus(0);

        assertThat(service.page(query).getRecords()).containsExactly("page");
        assertThat(service.detail(1L)).isEqualTo("detail-1");
        assertThat(service.create("create")).isEqualTo("created-create");
        assertThat(service.update(1L, "update")).isEqualTo("updated-1-update");
        service.changeStatus(1L, status);
        assertThat(service.statusChanged).isTrue();
    }

    private static class TestService implements BaseCrudService<Long, String, String, String, BasePageQueryDTO, BaseStatusDTO> {
        private boolean statusChanged;

        @Override
        public PageResult<String> page(BasePageQueryDTO query) {
            return PageResult.of(List.of("page"), 1L, 1L, 10L);
        }

        @Override
        public String detail(Long id) {
            return "detail-" + id;
        }

        @Override
        public String create(String createDTO) {
            return "created-" + createDTO;
        }

        @Override
        public String update(Long id, String updateDTO) {
            return "updated-" + id + "-" + updateDTO;
        }

        @Override
        public void changeStatus(Long id, BaseStatusDTO statusDTO) {
            statusChanged = true;
        }
    }
}
```

- [ ] **步骤 3：运行测试验证失败**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-common/daydayup-common-mybatis -am test
```

预期：编译失败，提示 `BaseCrudService` 不存在。

- [ ] **步骤 4：实现基础 CRUD 接口**

创建 `BaseCrudService.java`：

```java
package com.yuan.daydayup.common.mybatis.service;

import com.yuan.daydayup.common.core.dto.BasePageQueryDTO;
import com.yuan.daydayup.common.core.dto.BaseStatusDTO;
import com.yuan.daydayup.common.core.page.PageResult;

public interface BaseCrudService<ID, CreateDTO, UpdateDTO, VO, Q extends BasePageQueryDTO, S extends BaseStatusDTO> {

    PageResult<VO> page(Q query);

    VO detail(ID id);

    VO create(CreateDTO createDTO);

    VO update(ID id, UpdateDTO updateDTO);

    void changeStatus(ID id, S statusDTO);
}
```

- [ ] **步骤 5：实现可删除接口**

创建 `DeletableCrudService.java`：

```java
package com.yuan.daydayup.common.mybatis.service;

public interface DeletableCrudService<ID> {

    void delete(ID id);
}
```

- [ ] **步骤 6：实现抽象模板骨架**

创建 `AbstractCrudService.java`：

```java
package com.yuan.daydayup.common.mybatis.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.daydayup.common.core.dto.BasePageQueryDTO;
import com.yuan.daydayup.common.core.dto.BaseStatusDTO;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;

public abstract class AbstractCrudService<M extends BaseMapper<E>, E extends BaseEntity, ID, CreateDTO, UpdateDTO, VO, Q extends BasePageQueryDTO, S extends BaseStatusDTO>
        implements BaseCrudService<ID, CreateDTO, UpdateDTO, VO, Q, S> {

    protected final M mapper;

    protected AbstractCrudService(M mapper) {
        this.mapper = mapper;
    }

    protected void validateBeforeCreate(CreateDTO createDTO) {
    }

    protected void validateBeforeUpdate(ID id, UpdateDTO updateDTO) {
    }

    protected void validateBeforeStatusChange(ID id, S statusDTO) {
    }

    protected abstract E toEntity(CreateDTO createDTO);

    protected abstract void updateEntity(E entity, UpdateDTO updateDTO);

    protected abstract VO toVO(E entity);
}
```

- [ ] **步骤 7：运行测试验证通过**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-common/daydayup-common-mybatis -am test
```

预期：`AbstractCrudServiceTest` 通过。

- [ ] **步骤 8：Commit**

```bash
git add daydayup-common/daydayup-common-mybatis/pom.xml \
  daydayup-common/daydayup-common-mybatis/src/main/java/com/yuan/daydayup/common/mybatis/service \
  daydayup-common/daydayup-common-mybatis/src/test/java/com/yuan/daydayup/common/mybatis/service
git commit -m "feat(mybatis): add reusable crud service contracts"
```

---

## 任务 3：通配符权限匹配

**文件：**
- 修改：`daydayup-common/daydayup-common-security/pom.xml`
- 创建：`daydayup-common/daydayup-common-security/src/main/java/com/yuan/daydayup/common/security/permission/WildcardPermissionEvaluator.java`
- 创建：`daydayup-common/daydayup-common-security/src/main/java/com/yuan/daydayup/common/security/config/MethodSecurityConfig.java`
- 创建：`daydayup-common/daydayup-common-security/src/test/java/com/yuan/daydayup/common/security/permission/WildcardPermissionEvaluatorTest.java`

- [ ] **步骤 1：增加 common-security 测试依赖**

在 `daydayup-common/daydayup-common-security/pom.xml` 的 `<dependencies>` 内追加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **步骤 2：编写通配符匹配失败测试**

创建 `WildcardPermissionEvaluatorTest.java`：

```java
package com.yuan.daydayup.common.security.permission;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WildcardPermissionEvaluatorTest {

    @Test
    void shouldMatchSameAuthority() {
        assertThat(WildcardPermissionEvaluator.hasAuthority(
                List.of(new SimpleGrantedAuthority("admin:user:list")), "admin:user:list"))
                .isTrue();
    }

    @Test
    void shouldMatchWildcardAuthority() {
        assertThat(WildcardPermissionEvaluator.hasAuthority(
                List.of(new SimpleGrantedAuthority("admin:*")), "admin:user:list"))
                .isTrue();
    }

    @Test
    void shouldRejectDifferentPrefix() {
        assertThat(WildcardPermissionEvaluator.hasAuthority(
                List.of(new SimpleGrantedAuthority("game:*")), "admin:user:list"))
                .isFalse();
    }
}
```

- [ ] **步骤 3：运行测试验证失败**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-common/daydayup-common-security -am test
```

预期：编译失败，提示 `WildcardPermissionEvaluator` 不存在。

- [ ] **步骤 4：实现通配符匹配工具**

创建 `WildcardPermissionEvaluator.java`：

```java
package com.yuan.daydayup.common.security.permission;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public final class WildcardPermissionEvaluator {

    private WildcardPermissionEvaluator() {
    }

    public static boolean hasAuthority(Collection<? extends GrantedAuthority> authorities, String requiredAuthority) {
        if (authorities == null || requiredAuthority == null) {
            return false;
        }
        return authorities.stream().map(GrantedAuthority::getAuthority).anyMatch(authority -> matches(authority, requiredAuthority));
    }

    private static boolean matches(String authority, String requiredAuthority) {
        if (requiredAuthority.equals(authority)) {
            return true;
        }
        if (!authority.endsWith(":*")) {
            return false;
        }
        String prefix = authority.substring(0, authority.length() - 1);
        return requiredAuthority.startsWith(prefix);
    }
}
```

- [ ] **步骤 5：注册方法安全表达式处理器**

创建 `MethodSecurityConfig.java`，用自定义表达式根覆盖 `hasAuthority`。如 Spring Security 版本不允许直接替换表达式根，则改为注册 `PermissionEvaluator` 并把 Controller 权限注解写成 `@PreAuthorize("hasPermission(null, 'admin:user:list')")`。本项目优先保持 `hasAuthority` 语义。

```java
package com.yuan.daydayup.common.security.config;

import com.yuan.daydayup.common.security.permission.WildcardPermissionEvaluator;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.access.expression.method.MethodSecurityExpressionRoot;
import org.springframework.security.core.Authentication;

@Configuration(proxyBeanMethods = false)
public class MethodSecurityConfig {

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        return new DefaultMethodSecurityExpressionHandler() {
            @Override
            protected MethodSecurityExpressionOperations createSecurityExpressionRoot(Authentication authentication, MethodInvocation invocation) {
                return new WildcardMethodSecurityExpressionRoot(authentication);
            }
        };
    }

    private static class WildcardMethodSecurityExpressionRoot extends MethodSecurityExpressionRoot {

        WildcardMethodSecurityExpressionRoot(Authentication authentication) {
            super(authentication);
        }

        @Override
        public boolean hasAuthority(String authority) {
            return WildcardPermissionEvaluator.hasAuthority(getAuthentication().getAuthorities(), authority);
        }
    }
}
```

- [ ] **步骤 6：运行测试验证通过**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-common/daydayup-common-security -am test
```

预期：`WildcardPermissionEvaluatorTest` 通过，common-security 编译通过。

- [ ] **步骤 7：Commit**

```bash
git add daydayup-common/daydayup-common-security/pom.xml \
  daydayup-common/daydayup-common-security/src/main/java/com/yuan/daydayup/common/security/config/MethodSecurityConfig.java \
  daydayup-common/daydayup-common-security/src/main/java/com/yuan/daydayup/common/security/permission/WildcardPermissionEvaluator.java \
  daydayup-common/daydayup-common-security/src/test/java/com/yuan/daydayup/common/security/permission/WildcardPermissionEvaluatorTest.java
git commit -m "feat(security): support wildcard authorities"
```

---

## 任务 4：用户管理 API

**文件：**
- 修改：`daydayup-platform/daydayup-admin/daydayup-admin-biz/pom.xml`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/UserPageQueryDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/UserCreateDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/UserUpdateDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/UserStatusDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/vo/UserDetailVO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/service/UserManageService.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/service/impl/UserManageServiceImpl.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/controller/UserManageController.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/test/java/com/yuan/daydayup/admin/service/UserManageServiceImplTest.java`

- [ ] **步骤 1：增加 admin-biz 测试依赖**

在 `daydayup-platform/daydayup-admin/daydayup-admin-biz/pom.xml` 的 `<dependencies>` 内追加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **步骤 2：编写用户唯一性失败测试**

创建 `UserManageServiceImplTest.java`：

```java
package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.UserCreateDTO;
import com.yuan.daydayup.admin.entity.SysUser;
import com.yuan.daydayup.admin.mapper.SysUserMapper;
import com.yuan.daydayup.admin.service.impl.UserManageServiceImpl;
import com.yuan.daydayup.common.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserManageServiceImplTest {

    @Test
    void shouldRejectDuplicateUsername() {
        SysUserMapper mapper = mock(SysUserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(mapper.selectCount(any())).thenReturn(1L);
        UserManageServiceImpl service = new UserManageServiceImpl(mapper, encoder);
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("admin");
        dto.setPassword("admin123");
        dto.setNickname("管理员");

        assertThatThrownBy(() -> service.create(dto)).isInstanceOf(BizException.class);
    }
}
```

- [ ] **步骤 3：运行测试验证失败**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-platform/daydayup-admin/daydayup-admin-biz -am test -Dtest=UserManageServiceImplTest
```

预期：编译失败，提示用户管理 DTO 和 Service 不存在。

- [ ] **步骤 4：创建用户 DTO 与 VO**

创建 `UserPageQueryDTO.java`：

```java
package com.yuan.daydayup.admin.dto;

import com.yuan.daydayup.common.core.dto.BasePageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageQueryDTO extends BasePageQueryDTO {
    private String username;
    private String nickname;
    private Integer status;
}
```

创建 `UserCreateDTO.java`：

```java
package com.yuan.daydayup.admin.dto;

import com.yuan.daydayup.common.core.dto.BaseRequestDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserCreateDTO extends BaseRequestDTO {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private String nickname;
    private String email;
    private String mobile;
    private String avatar;
    private String remark;
}
```

创建 `UserUpdateDTO.java`：

```java
package com.yuan.daydayup.admin.dto;

import com.yuan.daydayup.common.core.dto.BaseRequestDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserUpdateDTO extends BaseRequestDTO {
    @NotBlank
    private String username;
    private String nickname;
    private String email;
    private String mobile;
    private String avatar;
    private String remark;
}
```

创建 `UserStatusDTO.java`：

```java
package com.yuan.daydayup.admin.dto;

import com.yuan.daydayup.common.core.dto.BaseStatusDTO;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class UserStatusDTO extends BaseStatusDTO {
}
```

创建 `UserDetailVO.java`：

```java
package com.yuan.daydayup.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserDetailVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String mobile;
    private String avatar;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private String remark;
}
```

- [ ] **步骤 5：实现用户 Service 接口与最小实现**

创建 `UserManageService.java`：

```java
package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.UserCreateDTO;
import com.yuan.daydayup.admin.dto.UserPageQueryDTO;
import com.yuan.daydayup.admin.dto.UserStatusDTO;
import com.yuan.daydayup.admin.dto.UserUpdateDTO;
import com.yuan.daydayup.admin.vo.UserDetailVO;
import com.yuan.daydayup.common.mybatis.service.BaseCrudService;

public interface UserManageService extends BaseCrudService<Long, UserCreateDTO, UserUpdateDTO, UserDetailVO, UserPageQueryDTO, UserStatusDTO> {
}
```

创建 `UserManageServiceImpl.java`，先实现 `create` 与唯一性校验，再补其他方法：

```java
package com.yuan.daydayup.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuan.daydayup.admin.dto.UserCreateDTO;
import com.yuan.daydayup.admin.dto.UserPageQueryDTO;
import com.yuan.daydayup.admin.dto.UserStatusDTO;
import com.yuan.daydayup.admin.dto.UserUpdateDTO;
import com.yuan.daydayup.admin.entity.SysUser;
import com.yuan.daydayup.admin.mapper.SysUserMapper;
import com.yuan.daydayup.admin.service.UserManageService;
import com.yuan.daydayup.admin.vo.UserDetailVO;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.core.page.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManageServiceImpl implements UserManageService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<UserDetailVO> page(UserPageQueryDTO query) {
        Page<SysUser> page = userMapper.selectPage(Page.of(query.normalizedPageNum(), query.normalizedPageSize()), new LambdaQueryWrapper<>());
        List<UserDetailVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public UserDetailVO detail(Long id) {
        SysUser user = requireUser(id);
        return toVO(user);
    }

    @Override
    public UserDetailVO create(UserCreateDTO dto) {
        ensureUsernameUnique(dto.getUsername(), null);
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setMobile(dto.getMobile());
        user.setAvatar(dto.getAvatar());
        user.setRemark(dto.getRemark());
        user.setStatus(1);
        userMapper.insert(user);
        return toVO(user);
    }

    @Override
    public UserDetailVO update(Long id, UserUpdateDTO dto) {
        ensureUsernameUnique(dto.getUsername(), id);
        SysUser user = requireUser(id);
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setMobile(dto.getMobile());
        user.setAvatar(dto.getAvatar());
        user.setRemark(dto.getRemark());
        userMapper.updateById(user);
        return toVO(user);
    }

    @Override
    public void changeStatus(Long id, UserStatusDTO dto) {
        SysUser user = requireUser(id);
        user.setStatus(dto.getStatus());
        userMapper.updateById(user);
    }

    private void ensureUsernameUnique(String username, Long excludeId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(SysUser::getId, excludeId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户名已存在");
        }
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户不存在");
        }
        return user;
    }

    private UserDetailVO toVO(SysUser user) {
        return UserDetailVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .remark(user.getRemark())
                .build();
    }
}
```

- [ ] **步骤 6：实现用户管理 Controller**

创建 `UserManageController.java`：

```java
package com.yuan.daydayup.admin.controller;

import com.yuan.daydayup.admin.dto.UserCreateDTO;
import com.yuan.daydayup.admin.dto.UserPageQueryDTO;
import com.yuan.daydayup.admin.dto.UserStatusDTO;
import com.yuan.daydayup.admin.dto.UserUpdateDTO;
import com.yuan.daydayup.admin.service.UserManageService;
import com.yuan.daydayup.admin.vo.UserDetailVO;
import com.yuan.daydayup.common.core.page.PageResult;
import com.yuan.daydayup.common.core.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/manage")
@RequiredArgsConstructor
public class UserManageController {

    private final UserManageService userManageService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('admin:user:list')")
    public R<PageResult<UserDetailVO>> page(@Valid UserPageQueryDTO query) {
        return R.ok(userManageService.page(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:user:detail')")
    public R<UserDetailVO> detail(@PathVariable Long id) {
        return R.ok(userManageService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin:user:create')")
    public R<UserDetailVO> create(@Valid @RequestBody UserCreateDTO dto) {
        return R.ok(userManageService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:user:update')")
    public R<UserDetailVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        return R.ok(userManageService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('admin:user:status')")
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        userManageService.changeStatus(id, dto);
        return R.ok();
    }
}
```

- [ ] **步骤 7：运行用户测试和编译**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-platform/daydayup-admin/daydayup-admin-biz -am test -Dtest=UserManageServiceImplTest
```

预期：测试通过，且 `UserManageController` 编译通过。

- [ ] **步骤 8：Commit**

```bash
git add daydayup-platform/daydayup-admin/daydayup-admin-biz/pom.xml \
  daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/User*DTO.java \
  daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/vo/UserDetailVO.java \
  daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/service/UserManageService.java \
  daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/service/impl/UserManageServiceImpl.java \
  daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/controller/UserManageController.java \
  daydayup-platform/daydayup-admin/daydayup-admin-biz/src/test/java/com/yuan/daydayup/admin/service/UserManageServiceImplTest.java
git commit -m "feat(admin): add user management api"
```

---

## 任务 5：角色、权限、菜单、字典、日志 API

**文件：**
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/RolePageQueryDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/RoleCreateDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/RoleUpdateDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/RoleStatusDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/PermissionPageQueryDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/PermissionCreateDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/PermissionUpdateDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/PermissionStatusDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/MenuPageQueryDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/MenuCreateDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/MenuUpdateDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/MenuStatusDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/DictPageQueryDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/DictCreateDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/DictUpdateDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/DictStatusDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/DictItemPageQueryDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/DictItemCreateDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/DictItemUpdateDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/DictItemStatusDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto/OperLogPageQueryDTO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/vo/RoleVO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/vo/PermissionVO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/vo/MenuVO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/vo/DictVO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/vo/DictItemVO.java`
- 创建：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/vo/OperLogVO.java`
- 创建：对应 `RoleService`、`PermissionService`、`MenuService`、`DictService`、`DictItemService`、`OperLogService` 及实现类。
- 创建：对应 `RoleController`、`PermissionController`、`MenuController`、`DictController`、`DictItemController`、`OperLogController`。
- 创建：对应 Service 测试。

- [ ] **步骤 1：编写角色 code 唯一性测试**

创建 `RoleServiceImplTest.java`，结构与用户唯一性测试一致，断言 `roleMapper.selectCount(any())` 返回 `1L` 时 `create` 抛出 `BizException`。

```java
package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.RoleCreateDTO;
import com.yuan.daydayup.admin.mapper.SysRoleMapper;
import com.yuan.daydayup.admin.service.impl.RoleServiceImpl;
import com.yuan.daydayup.common.core.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleServiceImplTest {

    @Test
    void shouldRejectDuplicateCode() {
        SysRoleMapper mapper = mock(SysRoleMapper.class);
        when(mapper.selectCount(any())).thenReturn(1L);
        RoleServiceImpl service = new RoleServiceImpl(mapper);
        RoleCreateDTO dto = new RoleCreateDTO();
        dto.setCode("super_admin");
        dto.setName("超级管理员");

        assertThatThrownBy(() -> service.create(dto)).isInstanceOf(BizException.class);
    }
}
```

- [ ] **步骤 2：实现角色 DTO、VO、Service、Controller**

创建角色 DTO 字段：`code/name/sort/status/remark`。Controller 路径为 `/roles`，权限码为 `admin:role:list/detail/create/update/status`。不创建 `delete` 方法。

关键 Service 规则：

```java
private void ensureCodeUnique(String code, Long excludeId) {
    LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, code);
    if (excludeId != null) {
        wrapper.ne(SysRole::getId, excludeId);
    }
    if (roleMapper.selectCount(wrapper) > 0) {
        throw new BizException(ErrorCode.BAD_REQUEST, "角色编码已存在");
    }
}
```

- [ ] **步骤 3：编写权限 code 唯一性测试**

创建 `PermissionServiceImplTest.java`，断言重复 `code` 抛出 `BizException`。

```java
package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.PermissionCreateDTO;
import com.yuan.daydayup.admin.mapper.SysPermissionMapper;
import com.yuan.daydayup.admin.service.impl.PermissionServiceImpl;
import com.yuan.daydayup.common.core.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionServiceImplTest {

    @Test
    void shouldRejectDuplicateCode() {
        SysPermissionMapper mapper = mock(SysPermissionMapper.class);
        when(mapper.selectCount(any())).thenReturn(1L);
        PermissionServiceImpl service = new PermissionServiceImpl(mapper);
        PermissionCreateDTO dto = new PermissionCreateDTO();
        dto.setCode("admin:user:list");
        dto.setName("用户列表");
        dto.setType("API");

        assertThatThrownBy(() -> service.create(dto)).isInstanceOf(BizException.class);
    }
}
```

- [ ] **步骤 4：实现权限 DTO、VO、Service、Controller**

创建权限 DTO 字段：`code/name/type/parentId/path/sort/status/remark`。Controller 路径为 `/permissions`，权限码为 `admin:permission:list/detail/create/update/status`。不创建 `delete` 方法。

- [ ] **步骤 5：编写菜单删除子节点测试**

创建 `MenuServiceImplTest.java`：

```java
package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.mapper.SysMenuMapper;
import com.yuan.daydayup.admin.service.impl.MenuServiceImpl;
import com.yuan.daydayup.common.core.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MenuServiceImplTest {

    @Test
    void shouldRejectDeleteWhenChildrenExist() {
        SysMenuMapper mapper = mock(SysMenuMapper.class);
        when(mapper.selectCount(any())).thenReturn(1L);
        MenuServiceImpl service = new MenuServiceImpl(mapper);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(BizException.class);
    }
}
```

- [ ] **步骤 6：实现菜单 DTO、VO、Service、Controller**

创建菜单 DTO 字段：`parentId/code/name/path/component/icon/type/permissionCode/sort/visible/status`。Controller 路径为 `/menus`，权限码为 `admin:menu:list/detail/create/update/status/delete`。实现 `DELETE /menus/{id}`，删除前检查子菜单数量。

关键删除规则：

```java
public void delete(Long id) {
    Long children = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
    if (children > 0) {
        throw new BizException(ErrorCode.BAD_REQUEST, "存在子菜单，不能删除");
    }
    menuMapper.deleteById(id);
}
```

- [ ] **步骤 7：实现字典与字典项**

创建字典 DTO 字段：`code/name/status/remark`。Controller 路径为 `/dicts`，权限码为 `admin:dict:list/detail/create/update/status/delete`。实现逻辑删除。

创建字典项 DTO 字段：`dictCode/value/label/sort/status/remark`。Controller 路径为 `/dict-items`，权限码为 `admin:dict-item:list/detail/create/update/status/delete`。唯一性校验使用 `(dictCode, value)`。

- [ ] **步骤 8：实现操作日志只读接口**

创建 `OperLogPageQueryDTO` 字段：`username/module/success/startTime/endTime`。创建 `OperLogVO` 字段映射 `SysOperLog`。Controller 路径为 `/oper-logs`，只提供：

```java
@GetMapping("/page")
@PreAuthorize("hasAuthority('admin:oper-log:list')")
public R<PageResult<OperLogVO>> page(@Valid OperLogPageQueryDTO query) {
    return R.ok(operLogService.page(query));
}

@GetMapping("/{id}")
@PreAuthorize("hasAuthority('admin:oper-log:detail')")
public R<OperLogVO> detail(@PathVariable Long id) {
    return R.ok(operLogService.detail(id));
}
```

不要创建 `POST`、`PUT`、`PATCH`、`DELETE`。

- [ ] **步骤 9：运行 admin-biz 测试**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-platform/daydayup-admin/daydayup-admin-biz -am test
```

预期：用户、角色、权限、菜单、字典、字典项、操作日志测试通过。

- [ ] **步骤 10：Commit**

```bash
git add daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/dto \
  daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/vo \
  daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/service \
  daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/controller \
  daydayup-platform/daydayup-admin/daydayup-admin-biz/src/test/java/com/yuan/daydayup/admin/service
git commit -m "feat(admin): add system management apis"
```

---

## 任务 6：权限种子数据与最终验证

**文件：**
- 修改：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/resources/db/init.sql`

- [ ] **步骤 1：补齐细粒度权限种子数据**

在 `sys_permission` 初始化数据中保留现有 `admin:*`，追加以下权限码：

```sql
INSERT INTO `sys_permission` (`id`, `code`, `name`, `type`, `sort`, `status`, `create_time`, `update_time`)
VALUES
    (1001, 'admin:user:list', '用户-列表', 'API', 1001, 1, NOW(), NOW()),
    (1002, 'admin:user:detail', '用户-详情', 'API', 1002, 1, NOW(), NOW()),
    (1003, 'admin:user:create', '用户-新增', 'API', 1003, 1, NOW(), NOW()),
    (1004, 'admin:user:update', '用户-修改', 'API', 1004, 1, NOW(), NOW()),
    (1005, 'admin:user:status', '用户-启停用', 'API', 1005, 1, NOW(), NOW()),
    (1011, 'admin:role:list', '角色-列表', 'API', 1011, 1, NOW(), NOW()),
    (1012, 'admin:role:detail', '角色-详情', 'API', 1012, 1, NOW(), NOW()),
    (1013, 'admin:role:create', '角色-新增', 'API', 1013, 1, NOW(), NOW()),
    (1014, 'admin:role:update', '角色-修改', 'API', 1014, 1, NOW(), NOW()),
    (1015, 'admin:role:status', '角色-启停用', 'API', 1015, 1, NOW(), NOW()),
    (1021, 'admin:permission:list', '权限-列表', 'API', 1021, 1, NOW(), NOW()),
    (1022, 'admin:permission:detail', '权限-详情', 'API', 1022, 1, NOW(), NOW()),
    (1023, 'admin:permission:create', '权限-新增', 'API', 1023, 1, NOW(), NOW()),
    (1024, 'admin:permission:update', '权限-修改', 'API', 1024, 1, NOW(), NOW()),
    (1025, 'admin:permission:status', '权限-启停用', 'API', 1025, 1, NOW(), NOW()),
    (1031, 'admin:menu:list', '菜单-列表', 'API', 1031, 1, NOW(), NOW()),
    (1032, 'admin:menu:detail', '菜单-详情', 'API', 1032, 1, NOW(), NOW()),
    (1033, 'admin:menu:create', '菜单-新增', 'API', 1033, 1, NOW(), NOW()),
    (1034, 'admin:menu:update', '菜单-修改', 'API', 1034, 1, NOW(), NOW()),
    (1035, 'admin:menu:status', '菜单-启停用', 'API', 1035, 1, NOW(), NOW()),
    (1036, 'admin:menu:delete', '菜单-删除', 'API', 1036, 1, NOW(), NOW()),
    (1041, 'admin:dict:list', '字典-列表', 'API', 1041, 1, NOW(), NOW()),
    (1042, 'admin:dict:detail', '字典-详情', 'API', 1042, 1, NOW(), NOW()),
    (1043, 'admin:dict:create', '字典-新增', 'API', 1043, 1, NOW(), NOW()),
    (1044, 'admin:dict:update', '字典-修改', 'API', 1044, 1, NOW(), NOW()),
    (1045, 'admin:dict:status', '字典-启停用', 'API', 1045, 1, NOW(), NOW()),
    (1046, 'admin:dict:delete', '字典-删除', 'API', 1046, 1, NOW(), NOW()),
    (1051, 'admin:dict-item:list', '字典项-列表', 'API', 1051, 1, NOW(), NOW()),
    (1052, 'admin:dict-item:detail', '字典项-详情', 'API', 1052, 1, NOW(), NOW()),
    (1053, 'admin:dict-item:create', '字典项-新增', 'API', 1053, 1, NOW(), NOW()),
    (1054, 'admin:dict-item:update', '字典项-修改', 'API', 1054, 1, NOW(), NOW()),
    (1055, 'admin:dict-item:status', '字典项-启停用', 'API', 1055, 1, NOW(), NOW()),
    (1056, 'admin:dict-item:delete', '字典项-删除', 'API', 1056, 1, NOW(), NOW()),
    (1061, 'admin:oper-log:list', '操作日志-列表', 'API', 1061, 1, NOW(), NOW()),
    (1062, 'admin:oper-log:detail', '操作日志-详情', 'API', 1062, 1, NOW(), NOW());
```

不要为超级管理员角色追加这些细粒度权限关系，超级管理员继续只绑定 `admin:*`。

- [ ] **步骤 2：全量编译和测试**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-platform/daydayup-admin/daydayup-admin-biz -am test
```

预期：相关模块测试全部通过。

- [ ] **步骤 3：检查用户、角色、权限没有删除接口**

运行：

```bash
grep -R "@DeleteMapping" daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/controller/UserManageController.java daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/controller/RoleController.java daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/controller/PermissionController.java
```

预期：无输出。

- [ ] **步骤 4：全项目编译**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -DskipTests package
```

预期：`BUILD SUCCESS`。

- [ ] **步骤 5：Commit**

```bash
git add daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/resources/db/init.sql
git commit -m "feat(admin): add granular admin permissions"
```

---

## 自检结果

- 规格覆盖度：计划覆盖分页对象、DTO 父类、Service 抽象、显式 Controller、通配符权限、用户/角色/权限禁删、菜单/字典逻辑删除、操作日志只读、权限种子数据与验证。
- 占位符扫描：计划没有使用实现占位符；所有示例方法都给出可执行的目标形态。
- 类型一致性：DTO、VO、Service、Controller 命名与规格一致；删除能力仅通过 `DeletableCrudService` 和允许删除的模块落地。
