package net.yellowstrawberry.youtubecli;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.YoutubeProgressCallback;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import me.tongfei.progressbar.ProgressBar;

public class Main {
    static YoutubeDownloader downloader = new YoutubeDownloader();
    static String v = "2.0.0";
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println(ConsoleColors.RESET)));
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));

        if(args.length < 1) {
            versionCheck();
            System.out.println("Possible arguments: ");
            System.out.println();
            System.out.println("--version                           > Show version & information of program");
            System.out.println("--update                            > Update this program");
            System.out.println("--type (video, audio, video-only)   > Set download type");
            System.out.println("(url)                               > Download youtube video");
            return;
        }

        List<String> a = new java.util.ArrayList<>(List.of(args));
        if(a.contains("--version")) {
            version();
            return;
        }else if(a.contains("--update")) {
            update();
            return;
        }

        int idx = a.indexOf("--type");
        if(idx == -1) {
            System.err.println("ERR: Type is not specified");
            System.exit(1);
        }
        a.remove(idx);

        if(idx+1 > args.length){
            System.err.println("ERR: Type is not specified");
            System.exit(1);
        }
        a.remove(idx);

        a.stream().findAny().map(s -> {
            String id = (s.toLowerCase().startsWith("http") ? (s.replaceFirst("http(s|)://www\\.youtube\\.com/shorts/", "").replaceFirst("http(s|)://www\\.youtube\\.com/watch\\?v=", "").replaceAll("&(.*)+=(.*)+", "")) : s);
            VideoInfo info = getVideoInfo(id);
            if(info == null) return null;

            Format f = parseFormat(args[idx+1], info);
            if(f == null) {
                System.err.printf("ERR: Unknown format `%s`%n", args[idx+1]);
                System.exit(1);
            }

            File outputDir = new File("./");

            String title = info.details().title();
            System.out.printf(ConsoleColors.BLUE_BRIGHT+"Downloading `%s`...%n"+ConsoleColors.RESET, title);
            try (ProgressBar pb1 = new ProgressBar("Downloading", 100)) {
                RequestVideoFileDownload request = new RequestVideoFileDownload(f)
                        .saveTo(outputDir)
                        .renameTo(title.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s", "_").replaceAll(" ", "_"))
                        .callback(new YoutubeProgressCallback<>() {
                            @Override
                            public void onDownloading(int progress) {
                                pb1.stepTo(progress);
                            }

                            @Override
                            public void onFinished(File f) {
                                pb1.close();
                                System.out.printf(ConsoleColors.GREEN + "Successfully downloaded file as `%s`!%n" + ConsoleColors.RESET, f.getAbsolutePath());
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                pb1.close();
                                System.err.println("ERR: " + throwable.getLocalizedMessage());
                            }
                        })
                        .async();
                Response<File> fr = downloader.downloadVideoFile(request);
                fr.data();
            }

            return null;
        }
        ).orElseGet(() -> {
           System.err.println("ERR: URL is not specified");
            System.exit(1);
           return java.util.Optional.empty();
        });
    }

    private static Format parseFormat(String f, VideoInfo info) {
        return (
                f.equalsIgnoreCase("audio") ? info.bestAudioFormat() : (
                        f.equalsIgnoreCase("video-only") ? info.bestVideoFormat() : (
                                f.equalsIgnoreCase("video") ? info.bestVideoWithAudioFormat() : null
                        )
                )
        );
    }

    private static void version() {
        String l = getLatestVersion();
        boolean isuptodate = l.equals(v);
        System.out.printf(
                "%sYoutube%s-%sCLI%s by %s@Yellowstrawberrys%s\n\n"+

                        "Version: %s%s%s\n"+
                        "Runtime: %s\n\n"+

                        "%s",

                ConsoleColors.RED, ConsoleColors.RESET, ConsoleColors.BLUE, ConsoleColors.RESET, ConsoleColors.YELLOW, ConsoleColors.RESET,
                (isuptodate?ConsoleColors.BLUE_BRIGHT:ConsoleColors.YELLOW), v, ConsoleColors.RESET,
                Runtime.version().toString(),
                (l.equals("nointernet")?ConsoleColors.RED+"Failed to connect to the update server.":isuptodate?ConsoleColors.GREEN+"Program is up-to-date!"
                        :ConsoleColors.YELLOW+"New update found! ('%s' -> '%s')".formatted(v, l))+ConsoleColors.RESET
        );
    }

    public static VideoInfo getVideoInfo(String videoId){
        RequestVideoInfo request = new RequestVideoInfo(videoId)
                .callback(new YoutubeCallback<>() {
                    @Override
                    public void onFinished(VideoInfo videoInfo) {
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.err.println("ERR: " + throwable.getMessage());
                        System.exit(1);
                    }
                })
                .async();
        Response<VideoInfo> response = downloader.getVideoInfo(request);
        return response.data();
    }

    private static final String verUrl = "https://raw.githubusercontent.com/Yellowstrawberrys/Youtube-CLI/main/version.json";
    private static final String upUrl = "https://github.com/Yellowstrawberrys/Youtube-CLI/releases/latest/download/";

    private static void versionCheck() {
        String latestVersion = getLatestVersion();
        if(!latestVersion.equals("nointernet") && !getLatestVersion().equals(v)) {
            System.out.printf(ConsoleColors.RED+"Your Youtube-CLI is outdated! (Current: %s, Latest: %s)%n", v, latestVersion);
            System.out.println("Type `youtube-cli --update` to update!"+ConsoleColors.RESET);
            System.out.println();
        }
    }

    private static void update() {
        System.out.println(ConsoleColors.CYAN+"Starting to update "+ConsoleColors.RESET+ConsoleColors.RED+"Youtube"+ConsoleColors.RESET+"-"+ConsoleColors.BLUE+"CLI"+ConsoleColors.RESET+"...");
        System.out.println();
        System.out.println(ConsoleColors.YELLOW+"Checking latest version..."+ConsoleColors.RESET);
        if(getLatestVersion().equals(v)) System.out.println(ConsoleColors.GREEN+"You're already up-to-date!");
        else {
            System.out.println(ConsoleColors.BLUE_BRIGHT+"Starting to downloading latest version...");
            boolean isw = System.getProperty("os.name").toLowerCase().startsWith("windows");
            URLConnection con = null;
            try {
                con = new URL(upUrl+(isw?"youtube-cli.exe":"youtube-cli")).openConnection();
            } catch (IOException e) {
                System.out.println(ConsoleColors.RED+"ERROR: "+e.getLocalizedMessage()+ConsoleColors.RESET);
            }
            con.setRequestProperty("User-Agent", RandomUserAgent.getRandomUserAgent());
            try(BufferedInputStream ipt = new BufferedInputStream(con.getInputStream()); ProgressBar progressBar = new ProgressBar("Updating", ipt.available()); FileOutputStream os = new FileOutputStream("youtube-cli%s.part".formatted(isw?".exe":""), false)) {
                progressBar.setExtraMessage("youtubecli%s.part".formatted(isw?".exe":""));

                int r;
                byte[] bf = new byte[1024];
                while ((r=ipt.read(bf)) != -1) {
                    os.write(bf, 0, r);
                    progressBar.stepBy(r);
                }
                os.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }finally {
                try {
                    String f = new File("./").getAbsolutePath();
                    f = f.replaceAll("\\\\", "/");
                    if(f.charAt(f.length()-1)=='.') f = f.substring(0, f.length()-2);
                    if(isw) {
                        new ProcessBuilder("cmd", "/c", "cd %s && ping 127.0.0.1 -n 1 -w 500> nul && del /F /Q youtubecli.exe && rename youtubecli.exe.part youtubecli.exe".formatted(f)).inheritIO().start();
                    }else {
                        new ProcessBuilder("/bin/bash", "-c", "cd %s && sleep .5 && rm -f youtube-cli && mv youtubecli.part youtubecli".formatted(f)).inheritIO().start();
                    }
                    System.out.println(ConsoleColors.GREEN+"Now, you're up-to-date!"+ConsoleColors.RESET);
                    System.exit(0);
                } catch (IOException e) {
                    System.err.println(e.getLocalizedMessage());
                }
            }
        }
    }

    private static String getLatestVersion() {
        try {
            return JsonReader.readFromUrl(verUrl).getString("version");
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            return "nointernet";
        }
    }
}
