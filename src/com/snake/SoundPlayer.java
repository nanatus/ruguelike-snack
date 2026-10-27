package com.snake;

/**
 * 音效播放器接口。加新的音源只要实现它，再让 {@link Sound} 认出来即可。
 */
interface SoundPlayer {

    /**
     * 播一个音效。
     *
     * @return true 表示真的出声了；false 表示这个音源处理不了这个音效，
     *         {@link Sound} 会当场回落到合成音，所以缺文件不会变成静音。
     */
    boolean play(Sfx sfx, float volume);

    void close();
}
