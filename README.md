## 秒杀系统实现记录



参考自：https://github.com/codingXiaxw/seckill

### 1. 业务流程描述

**用户成功秒杀商品，系统需要做的事：**

1、减库存；

2、记录用户的购买明细。（1.谁购买成功了。2.购买成功的时间/有效期。3.付款/发货信息）

**为什么我们的系统需要事务:**

1.用户成功秒杀商品我们记录了其购买明细却没有减库存，导致超卖。

2.减了库存却没有记录用户的购买明细，导致少卖。

**如何在保证事务的情况下，实现高并发？**

### 2.环境搭建

#### 2.1 添加依赖

单元测试、日志、数据库相关依赖、servlet web相关、mybatis、Spring核心依赖、Spring-dao、spring-web依赖

```xml
		<dependencies>
        <!--单元测试-->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.1</version>
        </dependency>
        <!--日志-->

        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>1.7.30</version>
        </dependency>

        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-core</artifactId>
            <version>1.2.3</version>
        </dependency>
        <!--实现slf4j接口整合-->

        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.2.3</version>
        </dependency>


        <!--数据库连接池-->
        <dependency>
            <groupId>c3p0</groupId>
            <artifactId>c3p0</artifactId>
            <version>0.9.1.2</version>
        </dependency>
        <!--数据库驱动-->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.22</version>
            <scope>runtime</scope>
        </dependency>
        <!--servlet web相关依赖-->

        <dependency>
            <groupId>taglibs</groupId>
            <artifactId>standard</artifactId>
            <version>1.1.2</version>
        </dependency>

        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>servlet-api</artifactId>
            <version>3.0-alpha-1</version>
        </dependency>

        <dependency>
            <groupId>javax.servlet.jsp</groupId>
            <artifactId>javax.servlet.jsp-api</artifactId>
            <version>2.3.3</version>
        </dependency>

        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>jstl</artifactId>
            <version>1.2</version>
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.11.2</version>
        </dependency>


        <!--SSM框架-->
        <dependency>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis</artifactId>
            <version>3.5.6</version>
        </dependency>
        <dependency>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis-spring</artifactId>
            <version>2.0.5</version>
        </dependency>

        <!--spring 依赖-->
        <!--spring核心依赖-->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-core</artifactId>
            <version>5.1.9.RELEASE</version>
        </dependency>

        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-beans</artifactId>
            <version>5.1.9.RELEASE</version>
        </dependency>

        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
            <version>5.1.9.RELEASE</version>
        </dependency>

        <!--spring-dao依赖-->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-jdbc</artifactId>
            <version>5.1.9.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-tx</artifactId>
            <version>5.1.9.RELEASE</version>
        </dependency>

        <!--spring web依赖-->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-webmvc</artifactId>
            <version>5.1.9.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
            <version>5.1.9.RELEASE</version>
        </dependency>
        <!--spring test依赖-->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-test</artifactId>
            <version>5.1.9.RELEASE</version>
        </dependency>


    </dependencies>
```

### 3. Dao层

#### 3.1 创建数据库和表

```sql
create database seckill;

use seckill;


create table `seckill`(
	`seckill_id` bigint not null auto_increment comment '商品库存ID',
	`name` varchar(120) not null comment '商品名称',
	`number` int not null comment '库存数量',
	`start_time` timestamp not null comment '秒杀开始时间',
	`end_time` timestamp not null comment '秒杀结束时间',
	`create_time` timestamp not null default current_timestamp comment '创建时间',
	primary key (seckill_id),
	key idx_start_time(start_time),
	key idx_end_time(end_time),
	key idx_create_time(create_time)
)engine=innodb auto_increment=1000 default charset=utf8 comment='秒杀库存表';

insert into seckill(name,number,start_time,end_time)
values
	('1000元秒杀iphone12', 100, '2020-10-30 00:00:00','2020-10-31 00:00:00'),
	('800元秒杀ipad pro', 200, '2020-10-30 00:00:00','2020-10-31 00:00:00'),
	('6600元秒杀iMac', 100, '2020-10-30 00:00:00','2020-10-31 00:00:00'),
	('7000元秒杀macbook pro', 100, '2020-10-30 00:00:00','2020-10-31 00:00:00');
	
--秒杀成功明细表
--用户登录认证相关信息：简化为手机号
create table success_killed(
	`seckill_id` bigint not null comment '秒杀商品ID',
	`user_phone` bigint not null comment '用户手机号',
	`state` tinyint not null default -1 comment '状态标识：-1:无效 0:成功 1:已付款 2:已发货',
	`create_time` timestamp not null default current_timestamp comment '创建时间',
	primary key(seckill_id, user_phone),/*联合主键？*/
	key idx_create_time(create_time)
)engine=innodb default charset=utf8 comment='秒杀成功明细表';
```





#### 3.2 创建实体类

```java
public class Seckill {
    private long seckill_id;
    private String name;
    private int number;
    private Date start_time;
    private Date end_time;
    private Date create_time;

    public Seckill(long seckill_id, String name, int number, Date start_time, Date end_time, Date create_time) {
        this.seckill_id = seckill_id;
        this.name = name;
        this.number = number;
        this.start_time = start_time;
        this.end_time = end_time;
        this.create_time = create_time;
    }

    public Seckill() {
    }

    @Override
    public String toString() {
        return "Seckill{" +
                "seckill_id=" + seckill_id +
                ", name='" + name + '\'' +
                ", number=" + number +
                ", start_time=" + start_time +
                ", end_time=" + end_time +
                ", create_time=" + create_time +
                '}';
    }

    public long getSeckill_id() {
        return seckill_id;
    }

    public void setSeckill_id(long seckill_id) {
        this.seckill_id = seckill_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public Date getStart_time() {
        return start_time;
    }

    public void setStart_time(Date start_time) {
        this.start_time = start_time;
    }

    public Date getEnd_time() {
        return end_time;
    }

    public void setEnd_time(Date end_time) {
        this.end_time = end_time;
    }

    public Date getCreate_time() {
        return create_time;
    }

    public void setCreate_time(Date create_time) {
        this.create_time = create_time;
    }
}
```

```java
public class SuccessKilled {
    private long seckill_id;
    private long user_phone;
    private short state;
    private Date create_time;

    public SuccessKilled(long seckill_id, long user_phone, short state, Date create_time) {
        this.seckill_id = seckill_id;
        this.user_phone = user_phone;
        this.state = state;
        this.create_time = create_time;
    }

    public SuccessKilled() {
    }

    @Override
    public String toString() {
        return "SuccessKilled{" +
                "seckill_id=" + seckill_id +
                ", user_phone=" + user_phone +
                ", state=" + state +
                ", create_time=" + create_time +
                '}';
    }

    public long getSeckill_id() {
        return seckill_id;
    }

    public void setSeckill_id(long seckill_id) {
        this.seckill_id = seckill_id;
    }

    public long getUser_phone() {
        return user_phone;
    }

    public void setUser_phone(long user_phone) {
        this.user_phone = user_phone;
    }

    public short getState() {
        return state;
    }

    public void setState(short state) {
        this.state = state;
    }

    public Date getCreate_time() {
        return create_time;
    }

    public void setCreate_time(Date create_time) {
        this.create_time = create_time;
    }
}

```





#### 3.3 创建dao接口

```java
public interface SeckillMapper {

    /**
     * 减库存的方法
     * @param seckill_id
     * @param kill_time
     * @return  表示更新库存的记录行数
     */
    public int reduceNumber(@Param("seckillId") long seckill_id,@Param("killTime") Date kill_time);

    /**
     * 根据id查询秒杀的商品信息
     * @param seckill_id
     * @return
     */
    public Seckill queryById(@Param("seckillId") long seckill_id);

    /**
     * 根据偏移量查询秒杀商品列表（什么偏移量？）
     * @param off
     * @param limit
     * @return
     */
    public List<Seckill> queryAll(@Param("offset") int off,@Param("limit") int limit);
}

```

```java
package com.yucaihuang.dao;

import com.yucaihuang.pojo.SuccessKilled;
import org.apache.ibatis.annotations.Param;

public interface SuccessKilledMapper {

    /**
     * 插入购买明细，可过滤重复
     * @param seckill_id
     * @param user_phone
     * @return  插入的行数
     */
    int insertSuccessKilled(@Param("seckillId") long seckill_id,@Param("userPhone") long user_phone);

    SuccessKilled queryByIdWithSeckill(@Param("seckillId") long seckill_id,@Param("userPhone") long user_phone);
}

```

#### 3.4 动态代理实现dao接口

mybatis全局配置文件`mybatis-config.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">

<configuration>

    <!--配置全局属性-->
    <settings>
        <!--使用jdbc的getGenerateKeys获取自增主键值-->
        <setting name="useGenerateKeys" value="true"/>
        <!--使用列别名替换列名
        开启后mybatis会自动帮我们把表中name的值赋到对应的实体的title属性中
        -->
        <setting name="useColumnLabel" value="true"/>
    </settings>

    <typeAliases>
        <package name="com.yucaihuang.pojo"/>
    </typeAliases>

    <mappers>
        <mapper class="com.yucaihuang.dao.SeckillMapper"/>
        <mapper class="com.yucaihuang.dao.SuccessKilledMapper"/>
    </mappers>
</configuration>
```

映射配置，必须与接口同名，但是这个Mapper文件存放的位置有两种形式：

* XxxMapper.xml和XxxMapper.java接口文件放在同个包下，即都放在`com.yucaihuang.dao`下：

  ![image-20201031150351335](https://hyc-pic.oss-cn-hangzhou.aliyuncs.com/image-20201031150351335.png)
  
  那么需要在pom.xml下加入以下配置，处理静态资源：
  
  ```xml
      <build>
          <resources>
              <resource>
                  <directory>src/main/java</directory>
                  <includes>
                      <include>**/*.xml</include>
                  </includes>
              </resource>
          </resources>
      </build>
  ```
  
  然后在mybatis-config.xml主配置文件中配置：
  
  ```xml
      <mappers>
          <mapper class="com.yucaihuang.dao.SeckillMapper"/>
          <mapper class="com.yucaihuang.dao.SuccessKilledMapper"/>
      </mappers>
  ```
  
  或者在后面的spring-dao.xml配置文件中配置：
  
  ```xml
      <bean class="org.mybatis.spring.SqlSessionFactoryBean" id="sqlSessionFactory">
          <property name="dataSource" ref="dataSource"/>
          <!--绑定mybatis配置文件，交给Spring管理-->
          <property name="configLocation" value="classpath:mybatis-config.xml"/>
          <property name="mapperLocations" value="classpath:Mapper/*.xml"/>
      </bean>
  ```
  
  
  
* 或者直接在resources目录下存放XxxMapper.xml文件：

  然后在mybatis-config.xml主配置文件中配置：

  ```xml
    <mappers>
          <mapper resource="Mapper/SeckillMapper.xml"/>
          <mapper resource="Mapper/SuccessKilledMapper.xml"/>
      </mappers>
  ```
  
  或者在后面的spring-dao.xml配置文件中配置：

  ```xml
    <bean class="org.mybatis.spring.SqlSessionFactoryBean" id="sqlSessionFactory">
          <property name="dataSource" ref="dataSource"/>
          <!--绑定mybatis配置文件，交给Spring管理-->
          <property name="configLocation" value="classpath:mybatis-config.xml"/>
          <property name="mapperLocations" value="classpath:Mapper/*.xml"/>
      </bean>
  ```
  
  

`SeckillMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.yucaihuang.dao.SeckillMapper">
    <update id="reduceNumber">
        update seckill.seckill
        set number = number-1
        where seckill_id=#{seckillId}
        and start_time <![CDATA[ <= ]]> #{killTime}
        and end_time >= #{killTime}
        and number > 0;

    </update>

    <select id="queryById" resultType="Seckill" parameterType="long">
        select * from seckill.seckill
        where seckill_id=#{seckillId};
    </select>

    <select id="queryAll" resultType="Seckill">
        select * from seckill.seckill
        order by create_time desc
        limit #{offset},#{limit}
    </select>
</mapper>
```

`SuccessKilledMapper.xml`:

```java
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.yucaihuang.dao.SuccessKilledMapper">
    <insert id="insertSuccessKilled" parameterType="long">
        <!--当出现主键冲突时（即重复秒杀时），会报错；不想让程序报错，就加入ignore???-->
        insert ignore into seckill.success_killed(seckill_id, user_phone,state)
        values (#{seckillId},#{userPhone},0);
    </insert>

    <select id="queryByIdWithSeckill" resultType="SuccessKilled">
        select
        sk.seckill_id,
        sk.user_phone,
        sk.create_time,
        sk.state,
        s.seckill_id "seckill.seckill_id",
        s.name "seckill.name",
        s.number "seckill",
        s.start_time "seckill.start_time",
        s.end_time "seckill.end_time",
        s.create_time "seckill.create_time"
        from seckill.success_killed sk
        inner join seckill.seckill s on sk.seckill_id=s.seckill_id
        where sk.seckill_id=#{seckillId}
        and sk.user_phone=#{userPhone};
    </select>

</mapper>
```



#### 3.5 整合spring和mybatis

编写`spring-dao.xml`，让Spring管理数据库连接池和dao接口的动态注入：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
        https://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context https://www.springframework.org/schema/context/spring-context.xsd">

    <!--关联数据库配置文件-->
    <context:property-placeholder location="classpath:jdbc.properties"/>

    <bean class="com.mchange.v2.c3p0.ComboPooledDataSource" id="dataSource">
        <property name="driverClass" value="${jdbc.driver}"/>
        <property name="jdbcUrl" value="${jdbc.url}"/>
        <property name="user" value="${jdbc.username}"/>
        <property name="password" value="${jdbc.password}"/>
        <!--c3p0私有属性-->
        <property name="maxPoolSize" value="30"/>
        <property name="minPoolSize" value="10"/>
        <!--关闭连接后不自动commit-->
        <property name="autoCommitOnClose" value="false"/>

        <!--获取连接超时时间-->
        <property name="checkoutTimeout" value="1000"/>
        <!--当获取连接失败重试次数-->
        <property name="acquireRetryAttempts" value="2"/>
    </bean>



    <bean class="org.mybatis.spring.SqlSessionFactoryBean" id="sqlSessionFactory">
        <property name="dataSource" ref="dataSource"/>
        <!--绑定mybatis配置文件，交给Spring管理-->
        <property name="configLocation" value="classpath:mybatis-config.xml"/>
        <property name="typeAliasesPackage" value="com.yucaihuang.pojo"/>
        <property name="mapperLocations" value="classpath:Mapper/*.xml"/>
    </bean>

    <!--实现dao接口动态注入-->
    <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
        <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory"/>
        <property name="basePackage" value="com.yucaihuang.dao"/>
    </bean>

</beans>
```



#### 3.6 dao层测试

测试SeckillMapper.java接口方法：

```java
/**
 * junt整合spring
 */
@RunWith(SpringJUnit4ClassRunner.class)
//告诉junit spring的配置文件
@ContextConfiguration("classpath:spring-dao.xml")
public class SeckillMapperTest {

    @Resource
    private SeckillMapper seckillMapper;

    @Test
    public void reduceNumber() {
        int i = seckillMapper.reduceNumber(1001, new Date());
        System.out.println(i);
    }

    @Test
    public void queryById() {
        long seckillId = 1001;
        Seckill seckill = seckillMapper.queryById(seckillId);
        System.out.println(seckill);
    }

    @Test
    public void queryAll() {
        List<Seckill> seckills = seckillMapper.queryAll(0, 100);
        for (Seckill seckill : seckills) {
            System.out.println(seckill);
        }
    }
}
```

三个方法均测试通过。

测试SuccessKilledMapper.java接口：

```java
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring-dao.xml")
public class SuccessKilledMapperTest {

    @Autowired
    private SuccessKilledMapper successKilledMapper;

    @Test
    public void insertSuccessKilled() {
        successKilledMapper.insertSuccessKilled(1001,12855555);
    }

    @Test
    public void queryByIdWithSeckill() {
        SuccessKilled successKilled = successKilledMapper.queryByIdWithSeckill(1001, 12855555);
        System.out.println(successKilled);
    }
}
```

两个方法均测试通过。



### 4. Service层

#### 4.1 创建业务层接口

编写业务层接口，有两个重要的方法：1、暴露秒杀接口的地址。2、处理秒杀

```java
/**
 * 业务层
 */
public interface SeckillService {

    /**
     * 查询全部的秒杀记录
     * @return
     */
    List<Seckill> getSeckillList();

    /**
     * 按ID查询秒杀记录
     * @return
     */
    Seckill getSeckillById(long seckillId);

    //往下是我们最重要的行为的一些接口

    /**
     * 在秒杀开启时输出秒杀接口的地址，否则输出系统时间和秒杀时间
     * @param seckillId
     * @return
     */
    Exposer exportSeckillUrl(long seckillId);

    /**
     * 执行秒杀操作，有可能失败，有可能成功，所以要抛出我们允许的异常
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    SeckillExecution executeSeckill(long seckillId, long userPhone, String md5)
            throws SeckillException, RepeatKillException, SeckillCloseException;
}

```

#### 4.2 dto封装类

建立一个包dto，用于封装业务层给web传输的数据，其中包括上面两个重要方法的返回值封装：`Exposer`和`SeckillExecution`：

```java
/**
 * 暴露秒杀地址(接口)DTO
 */
public class Exposer {

    //是否开启秒杀
    private boolean exposed;

    //对秒杀地址加密的措施
    private String md5;

    //id为seckillId的商品的秒杀地址
    private long seckillId;

    //系统当前时间（毫秒）
    private long now_time;

    //秒杀的开启时间
    private long start_time;

    //秒杀的结束时间
    private long end_time;

    public Exposer(boolean exposed, String md5, long seckillId) {
        this.exposed = exposed;
        this.md5 = md5;
        this.seckillId = seckillId;
    }

    public Exposer(boolean exposed, long seckillId, long now_time, long start_time, long end_time) {
        this.exposed = exposed;
        this.seckillId = seckillId;
        this.now_time = now_time;
        this.start_time = start_time;
        this.end_time = end_time;
    }

    public Exposer(boolean exposed, long seckillId) {
        this.exposed = exposed;
        this.seckillId = seckillId;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public long getSeckillId() {
        return seckillId;
    }

    public void setSeckillId(long seckillId) {
        this.seckillId = seckillId;
    }

    public long getNow_time() {
        return now_time;
    }

    public void setNow_time(long now_time) {
        this.now_time = now_time;
    }

    public long getStart_time() {
        return start_time;
    }

    public void setStart_time(long start_time) {
        this.start_time = start_time;
    }

    public long getEnd_time() {
        return end_time;
    }

    public void setEnd_time(long end_time) {
        this.end_time = end_time;
    }
}

```

```java
/**
 * 封装执行秒杀后的结果：是否秒杀成功
 */
public class SeckillExecution {

    private long seckillId;

    //秒杀执行结果的状态
    private int state;

    //状态的明文标识
    private String stateInfo;

    //当秒杀成功时，需要传递秒杀成功的对象回去
    private SuccessKilled successKilled;

    /**
     * 秒杀成功返回所有信息
     * @param seckillId
     * @param seckillStatEnum
     * @param stateInfo
     * @param successKilled
     */
    public SeckillExecution(long seckillId, SeckillStatEnum seckillStatEnum, String stateInfo, SuccessKilled successKilled) {
        this.seckillId = seckillId;
        this.state = seckillStatEnum.getState();
        this.stateInfo = stateInfo;
        this.successKilled = successKilled;
    }

    /**
     * 秒杀失败
     * @param seckillId
     * @param seckillStatEnum
     * @param stateInfo
     */
    public SeckillExecution(long seckillId, SeckillStatEnum seckillStatEnum, String stateInfo) {
        this.seckillId = seckillId;
        this.state = seckillStatEnum.getState();
        this.stateInfo = stateInfo;
    }

    public long getSeckillId() {
        return seckillId;
    }

    public void setSeckillId(long seckillId) {
        this.seckillId = seckillId;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getStateInfo() {
        return stateInfo;
    }

    public void setStateInfo(String stateInfo) {
        this.stateInfo = stateInfo;
    }

    public SuccessKilled getSuccessKilled() {
        return successKilled;
    }

    public void setSuccessKilled(SuccessKilled successKilled) {
        this.successKilled = successKilled;
    }
}
```



#### 4.3 异常处理类

创建一个exception包，用于处理异常，主要有两个异常：1、重复秒杀异常；2、秒杀结束异常。

```java
public class SeckillException extends RuntimeException {

    public SeckillException(String message) {
        super(message);
    }

    public SeckillException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

```java
package com.yucaihuang.exception;

/**
 * 重复秒杀异常，是一个运行时异常，不需要我们手动try catch
 * mysql只支持运行时异常的回滚操作
 */
public class RepeatKillException extends SeckillException {

    public RepeatKillException(String message) {
        super(message);
    }

    public RepeatKillException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

```java
package com.yucaihuang.exception;


/**
 * 秒杀关闭异常，当秒杀结束时，用户还要进行秒杀，就会出现这个异常
 */
public class SeckillCloseException extends SeckillException {
    public SeckillCloseException(String message) {
        super(message);
    }

    public SeckillCloseException(String message, Throwable cause) {
        super(message, cause);
    }
}
```



#### 4.4 业务层接口的实现

```java
public class SeckillServiceImpl implements SeckillService {


    //日志对象
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    //加入一个混淆字符串（秒杀接口）的salt，为了避免用户猜出我们的md5值，值任意给，越复杂越好
    private final String salt="safjlvllj`asdl.kn";

    private SeckillMapper seckillMapper;

    private SuccessKilledMapper successKilledMapper;

    public void setSeckillMapper(SeckillMapper seckillMapper) {
        this.seckillMapper = seckillMapper;
    }

    public void setSuccessKilledMapper(SuccessKilledMapper successKilledMapper) {
        this.successKilledMapper = successKilledMapper;
    }

    public List<Seckill> getSeckillList() {
        return seckillMapper.queryAll(0,4);
    }

    public Seckill getSeckillById(long seckillId) {
        return seckillMapper.queryById(seckillId);
    }

    /**
     * 根据seckillId来验证此产品是否在秒杀商品信息中，如果存在就判断当前时间是否在秒杀时间段内
     * 如果二者都成立，就生成一个加密后的md5，返回
     * @param seckillId
     * @return
     */
    public Exposer exportSeckillUrl(long seckillId) {
        Seckill seckill = seckillMapper.queryById(seckillId);
        //说明查不到这个秒杀产品的记录
        if(seckill == null){
            return new Exposer(false,seckillId);
        }
        Date start_time = seckill.getStart_time();
        Date end_time = seckill.getEnd_time();
        Date now_time = new Date();
        //若是当前时间不在秒杀时间段内
        if(start_time.getTime() > now_time.getTime() || end_time.getTime() < now_time.getTime()){
            return new Exposer(false, seckillId, now_time.getTime(), start_time.getTime(),end_time.getTime());
        }

        //秒杀开启，返回秒杀商品的id、用给接口加密的md5
        String md5 = getMD5(seckillId);
        return new Exposer(true, md5, seckillId);

    }

    private String getMD5(long seckillId){
        String base = seckillId + "/" + salt;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }

    /**
     * 秒杀是否成功，若成功：减库存，增加明细；失败：抛出异常，mysql自动事务回滚
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5)
            throws SeckillException, RepeatKillException, SeckillCloseException {

        if(md5 == null || !md5.equals((getMD5(seckillId)))){
            //md5不匹配，说明秒杀数据被重写了，抛出异常
            throw new SeckillException("seckill data has been rewrite");
        }

        Date now_time = new Date();

        try{
            //减库存
            int updateCount = seckillMapper.reduceNumber(seckillId, now_time);
            if(updateCount <= 0){
                //没有更新库存记录，说明秒杀结束
                throw  new SeckillCloseException("seckill is closed");
            }else {
                //成功更新了库存
                int insertCount = successKilledMapper.insertSuccessKilled(seckillId, userPhone);
                //是否该明细被重复插入，即用户是否重复秒杀
                if(insertCount <= 0){
                    throw new RepeatKillException("seckill repeated");
                }else {
                    SuccessKilled successKilled = successKilledMapper.queryByIdWithSeckill(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS,"秒杀成功",successKilled);
                }
            }
        }catch (SeckillCloseException e1){
            throw e1;
        }catch (RepeatKillException e2){
            throw e2;
        } catch (Exception e){
            logger.error(e.getMessage(),e);
            //编译期异常转化为运行期异常
            throw new SeckillException("seckill inner error :"+e.getMessage());
        }
    }
}

```

由于我们返回的数据是交给前端的，所以秒杀是否成功的状态我们封装到一个枚举类中：

```java
public enum  SeckillStatEnum {

    SUCCESS(1,"秒杀成功"),
    END(0,"秒杀结束"),
    REPEAT_KILL(-1,"重复秒杀"),
    INNER_ERROR(-2,"系统异常"),
    DATE_REWRITE(-3,"数据篡改");

    private int state;
    private String info;

    SeckillStatEnum(int state, String info) {
        this.state = state;
        this.info = info;
    }

    public int getState() {
        return state;
    }


    public String getInfo() {
        return info;
    }

    public static SeckillStatEnum stateOf(int index){
        for (SeckillStatEnum state : values()) {
            if(state.getState() == index){
                return state;
            }
        }
        return null;
    }
}
```



#### 4.5 将Service层交给Spring管理

创建`spring-service.xml`，配置扫描包，注入service的bean

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context" xmlns:tx="http://www.springframework.org/schema/tx"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
        https://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        https://www.springframework.org/schema/context/spring-context.xsd
        http://www.springframework.org/schema/tx 
        http://www.springframework.org/schema/tx/spring-tx.xsd">

    <context:component-scan base-package="com.yucaihuang.service"/>

    <bean class="com.yucaihuang.service.impl.SeckillServiceImpl" id="seckillServiceImpl">
        <property name="seckillMapper" ref="seckillMapper"/>
        <property name="successKilledMapper" ref="successKilledMapper"/>
    </bean>



    <!--事务-->
    <bean class="org.springframework.jdbc.datasource.DataSourceTransactionManager" id="transactionManager">
        <property name="dataSource" ref="dataSource"/>
    </bean>

</beans>
```





#### 4.6 使用Spring的声明式事务配置

声明式事务的使用方式:

1. 早期使用的方式:ProxyFactoryBean+XMl.
2. tx:advice+aop命名空间，这种配置的好处就是一次配置永久生效。
3. 注解@Transactional的方式。

在实际开发中，建议使用第三种对我们的事务进行控制。继续在`spring-service.xml`中配置：

```xml
    <!--配置基于注解的声明式事务-->
    <tx:annotation-driven transaction-manager="transactionManager"/>
```

然后在Service实现类方法中，在需要进行事务声明的方法上加上事务的注解：`@Transactional`

使用注解控制事务方法的优点：

* 开发团队达成一致约定，明确标注事务方法的编程风格；
* 保证事务方法的执行时间尽可能短，不要穿插其他网络操作RPC/HTTP请求或者剥离到事务方法外部；
* 不是所有的方法都需要事务，如果只有一条修改操作、只读操作不需要事务控制。



#### 4.7 Service逻辑的集成测试

```java
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:ApplicationContext.xml")
public class SeckillServiceTest {

    @Autowired
    SeckillService seckillService;

    @Test
    public void getSeckillList() {
        List<Seckill> seckillList = seckillService.getSeckillList();
        for (Seckill seckill : seckillList) {
            System.out.println(seckill);
        }
    }

    @Test
    public void getSeckillById() {
        Seckill seckillById = seckillService.getSeckillById(1001);
        System.out.println(seckillById);

    }

    @Test
    public void exportSeckillUrl() {
        Exposer exposer = seckillService.exportSeckillUrl(1002);
        System.out.println(exposer);
    }

    @Test
    public void executeSeckill() {
        SeckillExecution seckillExecution = seckillService.executeSeckill(1002, 1506779719, "80267e7716eeec0135c23d6a4a61add4");
        System.out.println(seckillExecution);
    }
}
```

当重复运行`executeSeckill`方法时，出现异常：

```shell
com.yucaihuang.exception.RepeatKillException: seckill repeated

	at com.yucaihuang.service.impl.SeckillServiceImpl.executeSeckill(SeckillServiceImpl.java:115)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:68)
	at 
	...
	com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:33)
	at com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:230)
	at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:58)

```

这是因为用户进行了重复秒杀，我们应该在该测试方法中添加try catch,将程序允许的异常包起来而不去向上抛给junit。

由上分析可知，第四个方法只有拿到了第三个方法暴露的秒杀商品的地址后才能进行测试，也就是说只有在第三个方法运行后才能运行测试第四个方法，而实际开发中我们不是这样的，需要将第三个测试方法和第四个方法合并到一个方法从而组成一个完整的逻辑流程:

```java
    @Test
    public void testSeckillSeckillLogic() throws Exception{
        long seckillId = 1002;
        long userPhone = 15067729719L;
        Exposer exposer = seckillService.exportSeckillUrl(seckillId);
        if(exposer.isExposed()){
            System.out.println(exposer);
            String md5 = exposer.getMd5();
            try {
                seckillService.executeSeckill(seckillId,userPhone,md5);
            }catch (RepeatKillException e1){
                throw e1;
            }catch (SeckillCloseException e2){
                throw e2;
            }
        }else {
            //秒杀未开启
            System.out.println(exposer);
        }
    }
```



### 5.mvc层

#### 5.1 整合spring

创建`spring-mvc.xml`配置文件，并开启注解模式、配置静态资源、扫描包、视图解析器

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:mvc="http://www.springframework.org/schema/mvc"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
        https://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        https://www.springframework.org/schema/context/spring-context.xsd
        http://www.springframework.org/schema/mvc
        https://www.springframework.org/schema/mvc/spring-mvc.xsd">

    <!--开启SpringMVC注解模式
    a. 自动注册DefaultAnnotationHanderMapping, AnnotationMethodHandlerAdapter
    b. 默认提供一系列的功能：数据绑定，数字和日期的format @NumberFormat, @DateTimeFormat
    c. xml, json的默认读写支持
    -->
    <mvc:annotation-driven/>

    <!--静态资源默认servlet配置-->
    <!--
    1. 加入对静态资源的处理：js, gif, png
    2. 允许使用"/"做整体映射
    -->
    <mvc:default-servlet-handler/>

    <!--扫描Controller-->
    <context:component-scan base-package="com.yucaihuang.controller"/>

    <!--视图解析器-->
    <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="viewClass" value="org.springframework.web.servlet.view.JstlView"/>

        <property name="prefix" value="/WEB-INF/jsp/"/>
        <property name="suffix" value=".jsp"/>
    </bean>

</beans>
```

写一个Controller先测试一下：

```java
@RequestMapping("/seckill")
public class SeckillController{
  	@RequestMapping("/hello")
  	public String hello(){
      	return "hello";
    }
}
```

请求：`localhost:8080/seckill/hello`测试成功。



#### 5.2 导入静态资源

将web目录下的文件拷贝到自己的web目录下。



#### 5.3 结果封装类

在dto包下创建`SeckillResult.java`类，用于封装md5地址和秒杀结果，给前端传值。

```java
/**
 * 将所有的ajax请求返回类型全部封装成json数据
 * @param <T>
 */
public class SeckillResult<T> {

    private boolean success;
    private T data;
    private String error;

    public SeckillResult(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    public SeckillResult(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}

```



#### 5.4 编写Controller方法

```java
@Controller
@RequestMapping("/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    /**
     * 展示秒杀列表
     * @param model
     * @return
     */
    @GetMapping("/list")
    public String list(Model model){
        List<Seckill> seckillList = seckillService.getSeckillList();
        model.addAttribute("seckillList",seckillList);
        return "list";
    }

    /**
     * 秒杀商品详情页
     * @param seckillId
     * @param model
     * @return
     */
    @GetMapping("/{seckillId}/detail")
    public String detail(@PathVariable("seckillId") Long seckillId, Model model){
        if(seckillId == null){
            return "redirect:/seckill/list";
        }

        Seckill seckill = seckillService.getSeckillById(seckillId);
        if(seckill == null){
            return "forward:/seckill/list";
        }
        model.addAttribute("seckill",seckill);

        return "detail";
    }

    /**
     * 返回一个JSON数据，数据中封装了我们商品的秒杀地址
     * @param seckillId
     * @return
     */
    @GetMapping(value = "/{seckillId}/exposer", produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<Exposer> exposer(@PathVariable("seckillId") Long seckillId){
        SeckillResult<Exposer> result;
        try {
            Exposer exposer = seckillService.exportSeckillUrl(seckillId);
            //成功取到了暴露的地址
            result = new SeckillResult<Exposer>(true, exposer);
        } catch (Exception e) {
            e.printStackTrace();
            //取地址失败了，封装异常信息
            result = new SeckillResult<Exposer>(false,e.getMessage());
        }
        return result;
    }

    /**
     * 用于封装用户是否秒杀成功的信息
     * @param secKillId
     * @param md5
     * @return
     */
    @PostMapping(value = "/{seckillId}/{md5}/execution",
    produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<SeckillExecution> execute(@PathVariable("seckillId") Long secKillId,
                                                   @PathVariable("md5") String md5,
                                                   @CookieValue(value = "userPhone", required = false) Long userPhone){

        if(userPhone == null){
            return new SeckillResult<SeckillExecution>(false,"未注册");
        }
        SeckillResult<SeckillExecution> result;

        try {
            SeckillExecution execution = seckillService.executeSeckill(secKillId, userPhone, md5);
            return new SeckillResult<SeckillExecution>(true,execution);
        }catch (RepeatKillException e1){
            SeckillExecution execution = new SeckillExecution(secKillId, SeckillStatEnum.REPEAT_KILL);
            return new SeckillResult<SeckillExecution>(true,execution);
        }catch (SeckillCloseException e2){
            SeckillExecution execution = new SeckillExecution(secKillId, SeckillStatEnum.END);
            return new SeckillResult<SeckillExecution>(true,execution);
        }catch (Exception e){
            SeckillExecution execution = new SeckillExecution(secKillId, SeckillStatEnum.INNER_ERROR);
            return new SeckillResult<SeckillExecution>(true,execution);
        }
    }

    /**
     * 返回系统当前时间
     * @return
     */
    @GetMapping("/time/now")
    @ResponseBody
    public SeckillResult<Long> time(){
        Date date = new Date();
        return new SeckillResult<Long>(true, date.getTime());
    }

}

```

* `@ResponseBody`注解表示该方法的返回结果直接写入 HTTP 响应正文中，一般在异步获取数据时使用；

- 在使用`@RequestMapping`后，返回值通常解析为跳转路径，加上`@Responsebody`后返回结果不会被解析为跳转路径，而是直接写入HTTP 响应正文中。例如，异步获取`json`数据，加上`@Responsebody`注解后，就会直接返回`json`数据。
- `@RequestBody`注解则是将 HTTP 请求正文插入方法中，使用适合的`HttpMessageConverter`将请求体写入某个对象。



#### 5.5 测试

秒杀商品列表：

![seckill_list](https://raw.githubusercontent.com/KolinHuang/seckill/master/Drawings/seckill_list.png)

秒杀商品详情信息：

![seckill_countdown](https://raw.githubusercontent.com/KolinHuang/seckill/master/Drawings/seckill_countdown.png)

![seckill_start](https://raw.githubusercontent.com/KolinHuang/seckill/master/Drawings/seckill_start.png)

秒杀成功：

![seckill_success](https://raw.githubusercontent.com/KolinHuang/seckill/master/Drawings/seckill_success.png)

重复秒杀：

![seckill_repeat](https://raw.githubusercontent.com/KolinHuang/seckill/master/Drawings/seckill_repeat.png)



### 6. 添加Redis缓存

#### 6.1 整合Dao层

在dao包中创建一个RedisMapper.java文件：

```java
public class RedisMapper {
    private final JedisPool jedisPool;


    public RedisMapper(String ip, int port){
        jedisPool = new JedisPool(ip,port);
    }

    //这是序列化吗
    private RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);

    public Seckill getSeckill(long seckillId){
        return getSeckill(seckillId,null);
    }

    /**
     * 从redis里读数据，如果不存在就返回null
     * @param seckillId
     * @param jedis
     * @return
     */
    public Seckill getSeckill(long seckillId, Jedis jedis){
        boolean hasJedis = jedis != null;

        try{
            if(!hasJedis){
                jedis = jedisPool.getResource();
            }
            try {
                String key = getSeckillRedisKey(seckillId);
                //根据key查询
                byte[] bytes = jedis.get(key.getBytes());
                //如果查到了，说明redis里有这个key的缓存，就反序列化，返回seckill对象
                if(bytes != null){
                    Seckill seckill = schema.newMessage();
                    ProtostuffIOUtil.mergeFrom(bytes, seckill,schema);
                    return seckill;
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                if(!hasJedis){
                    jedis.close();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private String getSeckillRedisKey(long seckillId){
        return "seckill:" + seckillId;
    }


    /**
     * 从redis中先读数据，如果没有，就从数据库中读
     * 这个Function挺有意思的，学习一下！
     * @param seckillId
     * @param getDataFromDb
     * @return
     */
    public Seckill getOrPutSeckill(long seckillId, Function<Long, Seckill> getDataFromDb){
        String lockKey = "seckill:locks:getSeckill:"+seckillId;
        String lockRequestId = UUID.randomUUID().toString();
        Jedis jedis = jedisPool.getResource();

        try{
            //循环争用锁，直到拿到了锁
            for(;;){
                Seckill seckill = getSeckill(seckillId, jedis);
                if(seckill != null){
                    return seckill;
                }
                //尝试获取锁
                boolean getLock = JedisUtils.tryGetDistributedLock(jedis,lockKey,lockRequestId,1000);
                if (getLock){
                    //获取到了锁,从数据库拿数据，存redis
                    seckill = getDataFromDb.apply(seckillId);
                    putSeckill(seckill, jedis);
                    return seckill;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            //无论如何都要把锁释放
            JedisUtils.releaseDistributedLock(jedis, lockKey, lockRequestId);
            jedis.close();
        }
        return null;
    }

    public String putSeckill(Seckill seckill) {
        return putSeckill(seckill, null);
    }

    //将Seckill对象序列化后，存入redis
    public String putSeckill(Seckill seckill, Jedis jedis){
        boolean hasJedis = jedis != null;
        try {
            if(!hasJedis){
                jedis = jedisPool.getResource();
            }
            try {
                String key = getSeckillRedisKey(seckill.getSeckill_id());
                byte[] bytes = ProtostuffIOUtil.toByteArray(seckill, schema, LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
                //超时缓存1小时
                int timeout = 60 * 60;
                String result = jedis.setex(key.getBytes(), timeout, bytes);
                return result;
            }finally {
                if(!hasJedis){
                    jedis.close();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

}

```

需要用到分布式锁，所以创建一个工具类`JedisUtils`，利用`set lock:xx true ex 5 nx`原子操作实现锁。

```java
public class JedisUtils {

    private static final String LOCK_SUCESS = "OK";
    private static final Long RELEASE_SUCESS = 1L;

    /**
     * 尝试获取分布式锁
     * @param jedis
     * @param lockKey
     * @param requestId
     * @param expireTime
     * @return
     */
    public static boolean tryGetDistributedLock(Jedis jedis, String lockKey,
                                                String requestId, int expireTime){
        SetParams setParams = new SetParams();
        setParams.nx();
        setParams.ex(expireTime);

        String result = jedis.set(lockKey,requestId,setParams);
        return LOCK_SUCESS.equals(result);
    }


    /**
     * 释放分布式锁
     * @param jedis
     * @param lockKey
     * @param requestId
     * @return
     */
    public static boolean releaseDistributedLock(Jedis jedis, String lockKey, String requestId){
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));

        return RELEASE_SUCESS.equals(result);
    }

}

```

在`spring-dao.xml`中配置bean:

```xml
    <!--redis-->
    <bean class="com.yucaihuang.dao.cache.RedisMapper" id="redisMapper">
        <constructor-arg index="0" value="118.31.103.27"/>
        <constructor-arg index="1" value="6379"/>
    </bean>
```





#### 6.2 整合Service层

注入`redisMapper`:

```java
    private RedisMapper redisMapper;
    public void setRedisMapper(RedisMapper redisMapper) {
        this.redisMapper = redisMapper;
    }
```

修改查询逻辑，优先查询Redis：

```java
    public Seckill getSeckillById(long seckillId) {
        return redisMapper.getOrPutSeckill(seckillId, new Function<Long, Seckill>() {
            public Seckill apply(Long id) {
                return seckillMapper.queryById(id);
            }
        });
    }
```

更新Service层依赖注入：

```xml
    <bean class="com.yucaihuang.service.impl.SeckillServiceImpl" id="seckillServiceImpl">
        <property name="seckillMapper" ref="seckillMapper"/>
        <property name="successKilledMapper" ref="successKilledMapper"/>
        <property name="redisMapper" ref="redisMapper"/>
    </bean>
```



遇到问题

```shell
Lookup method resolution failed; nested exception is java.lang.IllegalStateE
```

```shell
Resolution of declared constructors on bean Class [com.yucaihuang.dao.cache.RedisMapper] from ClassLoader [ParallelWebappClassLoader
```

是由于更新了pom.xml之后，没有在lib文件下加入依赖。具体：File -> Project Structure -> Artifacts -> WEB-INF -> lib。



测试：点击链接后，在redis中查询到了相应的键值。

