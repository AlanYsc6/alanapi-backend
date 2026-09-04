-- 创建库
create database if not exists alan;

-- 切换库
use alan;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userName     varchar(256)                           null comment '用户昵称',
    userAccount  varchar(256)                           not null comment '账号',
    phone        varchar(32)                            null comment '手机号（短信登录用）',
    email        varchar(256)                           null comment '邮箱（邮箱登录 / 重置密码用）',
    userAvatar   varchar(1024)                          null comment '用户头像',
    gender       tinyint                                null comment '性别',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user / admin',
    userPassword varchar(512)                           not null comment '密码',
    userStatus   int          default 0                 not null comment '账号状态（0-正常，1-冻结）',
    accessKey    varchar(512)                           null comment '开放平台调用凭证 accessKey',
    secretKey    varchar(512)                           null comment '开放平台密钥 secretKey（用于签名，需保密）',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    constraint uni_userAccount
        unique (userAccount),
    constraint uni_phone
        unique (phone),
    constraint uni_email
        unique (email)
) comment '用户';

-- 接口信息
create table if not exists alan.`interface_info`
(
    `id`            bigint auto_increment comment '主键' primary key,
    `name`          varchar(256)                       not null comment '名称',
    `description`   varchar(256)                       null comment '描述',
    `url`           varchar(512)                       not null comment '接口地址',
    `method`        varchar(256)                       not null comment '请求类型',
    `requestHeader` text                               null comment '请求头',
    `requestParams` text                               null comment '请求参数',
    `requestBody`   text                               null comment '请求体',
    `responseBody`  text                               null comment '响应体',
    `status`        int      default 0                 not null comment '接口状态（0-关闭，1-开启）',
    `userId`        bigint                             not null comment '创建人',
    `createTime`    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    `updateTime`    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    `isDelete`      tinyint  default 0                 not null comment '是否删除(0-未删, 1-已删)'
) comment '接口信息';

-- 用户调用接口关系表
create table if not exists alan.`user_interface_info`
(
    `id` bigint not null auto_increment comment '主键' primary key,
    `userId` bigint not null comment '调用用户 id',
    `interfaceInfoId` bigint not null comment '接口 id',
    `totalNum` int default 0 not null comment '总调用次数',
    `leftNum` int default 0 not null comment '剩余调用次数',
    `status` int default 0 not null comment '0-正常，1-禁用',
    `createTime` datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    `updateTime` datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    `isDelete` tinyint default 0 not null comment '是否删除',
    -- 同一用户对同一接口仅一条调用关系，支撑调用计数（接口服务验签后原子扣次 + 首调自动开通）
    unique key `uk_user_interface` (`userId`, `interfaceInfoId`)
) comment '用户调用接口关系';

-- 接口调用日志表（接口服务在每次实际调用后写入，平台侧仅查询）
create table if not exists alan.`invoke_log`
(
    `id` bigint not null auto_increment comment '主键' primary key,
    `userId` bigint not null comment '调用用户 id',
    `interfaceInfoId` bigint not null comment '接口 id（0-平台未登记的接口）',
    `requestPath` varchar(512) null comment '请求路径',
    `requestMethod` varchar(16) null comment '请求方式',
    `requestParams` text null comment '请求参数',
    `responseBody` text null comment '响应数据',
    `status` int default 0 not null comment '调用状态（0-失败，1-成功）',
    `costTime` bigint default 0 not null comment '耗时（毫秒）',
    `createTime` datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    `isDelete` tinyint default 0 not null comment '是否删除'
) comment '接口调用日志';

-- 首页文档
create table if not exists alan.`doc`
(
    `id`         bigint auto_increment comment '主键' primary key,
    `title`      varchar(256)                       not null comment '标题',
    `content`    text                               null comment '内容（支持 ## 小标题、``` 代码块）',
    `sort`       int      default 0                 not null comment '展示顺序（越小越靠前）',
    `createTime` datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    `updateTime` datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    `isDelete`   tinyint  default 0                 not null comment '是否删除(0-未删, 1-已删)'
) comment '首页文档';

-- SDK 下载
create table if not exists alan.`sdk`
(
    `id`          bigint auto_increment comment '主键' primary key,
    `name`        varchar(256)                       not null comment '名称',
    `version`     varchar(64)                        null comment '版本号',
    `description` varchar(512)                       null comment '说明',
    `fileUrl`     varchar(1024)                      not null comment '文件地址',
    `createTime`  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    `updateTime`  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    `isDelete`    tinyint  default 0                 not null comment '是否删除(0-未删, 1-已删)'
) comment 'SDK 下载';

-- 初始化首页文档（按标题防重，可重复执行）
insert into alan.`doc` (`title`, `content`, `sort`)
select '获取调用凭证',
       '登录平台后进入「密钥管理」页面，点击「生成密钥」即可获得属于你的 accessKey 和 secretKey。\naccessKey 用于标识身份，secretKey 只用于本地计算签名，请妥善保管，不要泄露给他人。重新生成后旧密钥立即失效。',
       1
from dual
where not exists (select 1 from alan.`doc` where `title` = '获取调用凭证');

insert into alan.`doc` (`title`, `content`, `sort`)
select '加密方式（签名算法）',
       '平台采用 HMAC-SHA256 签名认证，签名生成共三步：\n1. 组装参与签名的四个字段：accessKey、body、nonce、timestamp（sign 与 secretKey 本身不参与）；\n2. 按 key 的 ASCII 字典序升序排序，拼接为 k1=v1&k2=v2 形式的规范串（值为空的字段跳过）；\n3. 以 secretKey 为密钥，对规范串计算 HMAC-SHA256，输出小写十六进制字符串，即为 sign。\n\n注意：secretKey 只用于本地计算签名，绝不随请求发送；参与签名的 body 必须与实际发送的请求内容完全一致，否则验签失败。\n\n## 示例代码\n```\n// 表单接口：参与签名的是参数值；JSON 接口：原始请求体字符串\nString body = "alan";\nMap<String, String> headerMap = new HashMap<>();\nheaderMap.put("accessKey", accessKey);\nheaderMap.put("body", body);\nheaderMap.put("nonce", IdUtil.simpleUUID());\nheaderMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));\n// 1. 按 key 的 ASCII 字典序排序拼接规范串：\n//    accessKey=xxx&body=alan&nonce=xxx&timestamp=xxx\n// 2. 以 secretKey 为密钥计算 HMAC-SHA256，输出小写十六进制\nString sign = SignUtils.genSign(headerMap, secretKey);\nheaderMap.put("sign", sign);\n```',
       2
from dual
where not exists (select 1 from alan.`doc` where `title` = '加密方式（签名算法）');

insert into alan.`doc` (`title`, `content`, `sort`)
select '请求头说明',
       'accessKey：调用凭证，标识调用方身份，在「密钥管理」页生成（参与签名）\nbody：参与签名的请求内容，表单接口为参数值，JSON 接口为原始请求体（参与签名）\nnonce：随机字符串，每次请求重新生成，防止重放攻击（参与签名）\ntimestamp：秒级时间戳，与服务器时间误差需在 5 分钟内（参与签名）\nsign：签名 = HMAC-SHA256(规范串, secretKey)，小写十六进制（计算结果）',
       3
from dual
where not exists (select 1 from alan.`doc` where `title` = '请求头说明');

insert into alan.`doc` (`title`, `content`, `sort`)
select '服务端校验流程',
       '1. 校验五个请求头均不为空；\n2. 根据 accessKey 查询用户，不存在则拒绝；\n3. 校验 timestamp，与服务器时间相差超过 5 分钟视为过期请求；\n4. 校验 nonce：通过 Redis 登记随机串，时间窗口内重复出现视为重放攻击并拒绝；\n5. 用服务端保存的 secretKey 重新计算签名，与请求中的 sign 以常量时间比较，防止参数被篡改与时序攻击。任一步不通过均返回 403。',
       4
from dual
where not exists (select 1 from alan.`doc` where `title` = '服务端校验流程');

insert into alan.`doc` (`title`, `content`, `sort`)
select '调用示例',
       '使用官方 SDK（推荐）：\n```\nAlanApiClient client = new AlanApiClient(accessKey, secretKey);\n// GET 方式调用名称接口\nString result = client.getNameByGet("alan");\n// POST 方式调用名称接口\nString result2 = client.getNameByPost("alan");\n```\n原生 HTTP / curl（以 GET 表单接口为例）：\n```\nAK="你的accessKey"; SK="你的secretKey"\nBODY="alan"                                   # 表单接口：参与签名的是参数值\nNONCE=$(uuidgen | tr -d "-")\nTS=$(date +%s)\nSIGN=$(printf "accessKey=%s&body=%s&nonce=%s&timestamp=%s" "$AK" "$BODY" "$NONCE" "$TS" | openssl dgst -sha256 -hmac "$SK" | sed "s/.*=//")\ncurl "http://localhost:8123/api/name/?name=${BODY}" -H "accessKey: ${AK}" -H "body: ${BODY}" -H "nonce: ${NONCE}" -H "timestamp: ${TS}" -H "sign: ${SIGN}"\n```',
       5
from dual
where not exists (select 1 from alan.`doc` where `title` = '调用示例');

insert into alan.`doc` (`title`, `content`, `sort`)
select '常见错误排查',
       '调用返回 403「无权限」时，请依次排查：\n1. accessKey 是否正确、密钥是否已被重新生成（旧密钥立即失效）；\n2. timestamp 是否为秒级、本机时间偏差是否超过 5 分钟；\n3. nonce 是否重复使用（每次请求都要生成新的随机串）；\n4. 参与签名的 body 是否与实际请求内容完全一致；\n5. 规范串拼接顺序是否按 key 字典序、签名是否为小写十六进制。',
       6
from dual
where not exists (select 1 from alan.`doc` where `title` = '常见错误排查');

