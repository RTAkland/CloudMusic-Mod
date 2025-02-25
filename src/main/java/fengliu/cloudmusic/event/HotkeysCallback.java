package fengliu.cloudmusic.event;

import fengliu.cloudmusic.command.MusicCommand;
import fengliu.cloudmusic.config.ConfigGui;
import fengliu.cloudmusic.config.Configs;
import fengliu.cloudmusic.music163.IMusic;
import fengliu.cloudmusic.music163.data.Music;
import fengliu.cloudmusic.util.MusicPlayer;
import fengliu.cloudmusic.util.page.Page;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class HotkeysCallback implements IHotkeyCallback {
    private final MinecraftClient client = MinecraftClient.getInstance();

    private interface Job {
        void fun (MinecraftClient client);
    }

    private void runHotKey(Job job){
        MinecraftClient mcClient = client;
        Thread commandThread = new Thread(){
            @Override
            public void run() {
                try {
                    job.fun(mcClient);
                } catch (Exception err) {
                    if (mcClient.player == null){
                        return;
                    }

                    mcClient.player.sendMessage(Text.literal(err.getMessage()), false);
                }
            }
        };
        commandThread.setDaemon(true);
        commandThread.setName("CloudMusic HotKey Thread");
        commandThread.start();
    }

    public static void init(){
        HotkeysCallback hotkeysCallback = new HotkeysCallback();

        for(ConfigHotkey hotkey: Configs.HOTKEY.HOTKEY_LIST){
            hotkey.getKeybind().setCallback(hotkeysCallback);
        }
    }

    @Override
    public boolean onKeyAction(KeyAction action, IKeybind key) {

        if (key == Configs.HOTKEY.OPEN_CONFIG_GUI.getKeybind() && action == KeyAction.PRESS){
            client.setScreen(new ConfigGui());
            return true;
        }

        if (key == Configs.HOTKEY.SWITCH_PLAY_MUSIC.getKeybind() && action == KeyAction.PRESS){
            MusicPlayer player = MusicCommand.getPlayer();
            if (player.getPlayingMusic() == null){
                return true;
            }

            if (!player.isPlaying()){
                player.continues();
                return true;
            }

            player.stop();
            return true;
        }

        if (key == Configs.HOTKEY.PLAY_VOLUME_ADD.getKeybind() && action == KeyAction.PRESS){
            MusicCommand.getPlayer().volumeAdd();
            if (client.player != null){
                client.player.sendMessage(Text.translatable("cloudmusic.info.hotkey.play.volume.add", Configs.PLAY.VOLUME.getStringValue()), true);
            }
            return true;
        }

        if (key == Configs.HOTKEY.PLAY_VOLUME_DOWN.getKeybind() && action == KeyAction.PRESS){
            MusicCommand.getPlayer().volumeDown();
            if (client.player != null){
                client.player.sendMessage(Text.translatable("cloudmusic.info.hotkey.play.volume.down", Configs.PLAY.VOLUME.getStringValue()), true);
            }
            return true;
        }

        if (key == Configs.HOTKEY.PLAY_MUSIC.getKeybind() && action == KeyAction.PRESS){
            MusicCommand.getPlayer().continues();
            return true;
        }

        if (key == Configs.HOTKEY.NEXT_MUSIC.getKeybind() && action == KeyAction.PRESS){
            MusicCommand.getPlayer().next();
            return true;
        }

        if (key == Configs.HOTKEY.PREV_MUSIC.getKeybind() && action == KeyAction.PRESS){
            MusicCommand.getPlayer().prev();
            return true;
        }

        if (key == Configs.HOTKEY.STOP_MUSIC.getKeybind() && action == KeyAction.PRESS){
            MusicCommand.getPlayer().stop();
            return true;
        }

        if (key == Configs.HOTKEY.EXIT_PLAY.getKeybind() && action == KeyAction.PRESS){
            MusicCommand.getPlayer().exit();
            return true;
        }

        if (key == Configs.HOTKEY.DELETE_PLAY_MUSIC.getKeybind() && action == KeyAction.PRESS){
            MusicCommand.getPlayer().deletePlayingMusic();
            return true;
        }

        if (key == Configs.HOTKEY.PLAYLIST_RANDOM.getKeybind() && action == KeyAction.PRESS){
            MusicCommand.getPlayer().randomPlay();
            return true;
        }

        if (key == Configs.HOTKEY.TRASH_ADD_PLAY_MUSIC.getKeybind() && action == KeyAction.PRESS){
            MusicPlayer player = MusicCommand.getPlayer();
            IMusic music = player.getPlayingMusic();
            if (!(music instanceof Music)){
                return true;
            }

            player.deletePlayingMusic();
            this.runHotKey(mc -> {
                ((Music) music).addTrashCan();

                if (mc.player != null){
                    mc.player.sendMessage(Text.translatable("cloudmusic.info.command.trash", music.getName()), false);
                }
            });
        }

        if (key == Configs.HOTKEY.LIKE_MUSIC.getKeybind() && action == KeyAction.PRESS){
            IMusic music = MusicCommand.getPlayer().getPlayingMusic();
            if (!(music instanceof Music)){
                return true;
            }

            this.runHotKey(mc -> {
                ((Music) music).like();

                if (mc.player != null){
                    mc.player.sendMessage(Text.translatable("cloudmusic.info.command.music.like", music.getName()), false);
                }
            });
            return true;
        }

        if (key == Configs.HOTKEY.PLAYLIST_ADD_MUSIC.getKeybind() && action == KeyAction.PRESS){
            IMusic music = MusicCommand.getPlayer().getPlayingMusic();
            if (!(music instanceof Music)){
                return true;
            }

            this.runHotKey(mc -> {
                Page page = MusicCommand.getMy(false).playListSetMusic(music.getId(), "add");
                page.setInfoText(Text.translatable("cloudmusic.info.page.user.playlist.add", MusicCommand.getMy(false).name));
                MusicCommand.setPage(page);
                page.look();
            });
            return true;
        }

        if (key == Configs.HOTKEY.PLAYLIST_DEL_MUSIC.getKeybind() && action == KeyAction.PRESS){
            IMusic music = MusicCommand.getPlayer().getPlayingMusic();
            if (!(music instanceof Music)){
                return true;
            }

            this.runHotKey(mc -> {
                Page page = MusicCommand.getMy(false).playListSetMusic(music.getId(), "del");
                page.setInfoText(Text.translatable("cloudmusic.info.page.user.playlist.del", MusicCommand.getMy(false).name));
                MusicCommand.setPage(page);
                page.look();
            });
            return true;
        }

        return false;
    }

}
