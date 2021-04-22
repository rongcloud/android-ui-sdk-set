package io.rong.imkit.conversation.extension;

public enum InputMode {
    /*文本输入状态*/
    TextInput,
    /*语音输入状态*/
    VoiceInput,
    /*表情输入状态，点击输入栏笑脸图标后触发*/
    EmoticonMode,
    /*插件输入状态，点击输入栏加号图标后触发*/
    PluginMode,
    /*更多输入状态，长按消息，弹出框里点击"更多"后触发*/
    MoreInputMode,
    /*语音识别输入状态，点击语音输入plugin后触发*/
    RecognizeMode

}
