# IMKit-Android
本项目是融云 IMKit SDK 组件的开源代码，IMKit SDK 是对融云 IM 即时通讯能力在 UI 界面的封装，方便开发者快速实现自己的产品，主要特点是支持快速集成，提供丰富的界面定制功能。

### 使用说明

本项目提供的是 library 组件，需要将 'kit' 部分源码集成到您的工程里方可运行。

详细说明请参考 [SDK 源码集成方式说明](https://sealtalk-custom.rongcloud.net/v4/5X/views/im/ui/guide/quick/include/android.html#source)

:::

温馨提示：

1.强烈不建议直接修改源码内容，防止后续源码升级将修改内容覆盖

2.建议通过继承重写某些类与自身逻辑不一致的方法，来增加新方法以扩展自身的业务逻辑

3.如果需要修改资源文件，建议将 SDK 里的资源文件拷贝到应用特定目录下，在 gradle 里配置优先使用特定目录下的资源文件即可。

3.建议使用 SDK 对外暴露的接口，如果调用私有接口可能会出现版本升级引起私有接口变更

:::

### 源码导读

#### 整体架构

IMKit 整体采用 MVVM 框架，依赖 androidx 组件，数据库部分由 Room 实现。

![Image text](https://github.com/rongcloud/imkit-android/blob/main/images/imkit.png)

各层级详细说明如下：

| 层级         | 包含组件                                                     | 作用                                                         |
| ------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| View 层      | Activity & Fragment<br />各类型会话展示模板(*extends BaseConversationProvider) <br />各类型消息展示模板( *extends BaseMessageItemProvider)<br />输入框扩展模块（RongExtension）<br />输入框扩展插件（Plugin） | 1. View 层监听 ViewModel 的 LiveData 的变化，进行页面刷新。<br /><br />2. View 层调用 ViewModel 获取业务数据 |
| ViewModel 层 | ConversationListViewModel<br />MessageListViewModel<br />RongExtensionViewModel<br /><br />UserInfoViewModel | 1. ViewModel 根据上层业务需要，获取数据仓库里对应的数据，返回给 UI。<br />2. ViewModel 监听数据仓库里的变化，通过 LiveData 通知各页面<br />3. ViewModel 调用各功能中心对应的功能函数进行业务处理，并监听状态的变化，返回给 UI 层 |
| Model 层     | IMCenter<br />RongUserInfoManager<br />RongConfigCenter      | 数据存储层，为各 ViewModel 提供数据。                        |

#### 核心类说明

开发者使用过程中主要涉及到以下核心类：

| 核心类              | 说明                                                         |
| ------------------- | ------------------------------------------------------------ |
| RongIM              | IM 核心类                                                    |
| RongUserInfoManager | 用户信息相关方法的统一入口类                                 |
| RongConfigCenter    | 配置中心，各业务层由此获取配置数据。                         |
| RouteUtils          | activity 路由器，封装了 SDK 内部各页面跳转方法，业务层调用相关方法即可，避免了重复的 intent 封装。 |

#### 基础模块架构

##### 配置中心

![image](./images/config.svg)

##### 会话列表

![image](./images/conversationlist.svg)

##### 会话页面

会话页面由消息列表和输入扩展栏两部分组成，下面分成两部分组件分别描述。

消息列表组件：

![image](./images/messagelist.svg)

输入扩展栏：

![image](./images/extension.svg)

#### 扩展功能说明

IMKit 里包含了很多扩展功能，如转发、@功能、快捷回复等，这些扩展功能通过监听基础模块的事件和对基础模块的容器进行操作而实现，各基础模块对扩展功能没有强耦合关系。

扩展功能详细列表请参考下面目录结构 feature 文件夹下的说明。

#### 目录结构说明
```
├── IMCenter.java  (IM 核心类)  
├── RongIM.java   (兼容老版本保留类)  
├── activity (SDK 内部 activity)  
├── config (配置相关类)  
│   ├── ConversationConfig.java  
│   ├── ConversationListConfig.java  
│   ├── FeatureConfig.java  
│   ├── GatheredConversationConfig.java  
│   └── RongConfigCenter.java  
├── conversation (会话页相关类)  
│   ├── ConversationFragment.java  
│   ├── ConversationViewModel.java  
│   ├── MessageListAdapter.java  
│   ├── RongConversationActivity.java  
│   ├── extension (输入区)  
│   │   ├── DefaultExtensionConfig.java (输入区默认配置)  
│   │   ├── InputMode.java (输入模式)  
│   │   ├── RongExtension.java (View 层，处理输入区 UI)  
│   │   ├── RongExtensionCacheHelper.java (输入区数据存储类)  
│   │   ├── RongExtensionManager.java (各输入扩展模块管理类)  
│   │   ├── RongExtensionViewModel.java (ViewModel，业务数据处理)  
│   │   └── component (内部基础组件)  
│   │       ├── emoticon  
│   │       ├── inputpanel  
│   │       ├── moreaction  
│   │       └── plugin  
│   └── messgelist  
│       ├── processor (业务处理器)  
│       ├── provider  (各消息模板)  
│       ├── status (消息列表状态)  
│       └── viewmodel  
├── conversationlist  
│   ├── ConversationListAdapter.java  
│   ├── ConversationListFragment.java  
│   ├── RongConversationListActivity.java  
│   ├── model  
│   │   ├── BaseUiConversation.java     (ui 会话基类)  
│   │   ├── GatheredConversation.java   (聚合会话)  
│   │   ├── GroupConversation.java      (群组会话)  
│   │   ├── PublicServiceConversation.java  (公众号会话)  
│   │   └── SingleConversation.java  (单一用户会话，比如单聊、客服、系统会话)  
│   ├── provider  (模板类)  
│   └── viewmodel  
│       ├── ConversationListViewModel.java(会话列表 ViewModel)  
├── event     (内部事件)  
│   ├── Event.java  
│   ├── actionevent   (业务类事件)  
│   └── uievent       (UI 类事件)  
├── feature (基于基础模块开发的各拓展功能)  
│   ├── customservice  (客服)  
│   ├── destruct       (阅后即焚)  
│   ├── forward        (转发)  
│   ├── location       (位置、实时位置)  
│   ├── mention        (At 功能)  
│   ├── publicservice  (公众号)  
│   ├── quickreply     (快捷回复)  
│   ├── recallEdit     (撤回重新编辑)  
│   ├── reference      (消息引用)  
│   └── resend         (消息失败重发)  
├── manager     (各管理类)  
├── model  
├── notification  (本地通知相关)  
├── picture       (图片相关)  
├── subconversationlist  (聚合会话列表相关类)  
├── userinfo  (用户信息相关类)  
├── utils  (帮助类)  
└── widget  
```