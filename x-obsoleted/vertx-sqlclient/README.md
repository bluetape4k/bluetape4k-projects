# Module Vert.x Sql Client Example

English | [한국어](./README.ko.md)

> **⚠️ Obsolete**: This module has been merged into `bluetape4k-vertx` and fully excluded from the build.

An example of using [Vert.x Sql Client](https://vertx.io/docs/vertx-sql-client/java/) together with [MyBatis Dynamic SQL](https://mybatis.org/mybatis-dynamic-sql/docs/introduction.html) to access a database in an async/non-blocking fashion.

This approach combines MyBatis's SQL Mapper capabilities with the Vert.x SQL Client.

## Mapping with Vert.x Data Objects

Instead of using the Vert.x SqlClient `RowMapper`, you can map rows directly to DTOs. See the `UserDataObject` implementation in the `dataobject` folder for reference.

1. Add `io.vertx:vertx-codegen:4.3.1:processor` to your dependencies:

```
compileOnly(Libs.vertx_codegen)
kapt(Libs.vertx_codegen)
kaptTest(Libs.vertx_codegen)
```

2. Add a `package-info.java` to the module:

[package-info.java](src/main/java/io/bluetape4k/workshop/sqlclient/package-info.java)

```java
@ModuleGen(name = "vertx-sqlclient-demo", groupPackage = "io.bluetape4k.workshop.sqlclient")
package io.bluetape4k.workshop.sqlclient;

import io.vertx.codegen.annotations.ModuleGen;
```

Reference:
[Mapping with Vert.x data objects](https://vertx.io/docs/vertx-sql-client-templates/java/#_mapping_with_vert_x_data_objects)
