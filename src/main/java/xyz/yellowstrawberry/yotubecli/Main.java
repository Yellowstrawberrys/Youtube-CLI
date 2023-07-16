package xyz.yellowstrawberry.yotubecli;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

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
    static String v = Main.class.getPackage().getImplementationVersion();
    public static void main(String[] args) {
        if(args.length < 1) {
            versionCheck();
            System.out.println("This program requires more than 1 argument(s).");
            System.out.println("Arguments: (url) [audio/video/video-only]");
        }else if(args[0].equals("--version")) {
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
        }else if(args[0].equals("--update")) {
            update();
        }else if(args[0].toLowerCase().startsWith("http://") || args[0].toLowerCase().startsWith("https://")){
            versionCheck();
            File outputDir = new File("./");
            VideoInfo info = getVideoInfo(args[0].split("\\?v=")[1]);

            String title = info.details().title();

            System.out.printf(ConsoleColors.BLUE_BRIGHT+"Starting to download `%s`...%n"+ConsoleColors.RESET, title);

            Format format = (args.length == 2 ? (
                    args[1].equalsIgnoreCase("audio") ? info.bestAudioFormat() : (
                            args[1].equalsIgnoreCase("video-only") ? info.bestVideoFormat() : (
                                    args[1].equalsIgnoreCase("video") ? info.bestVideoWithAudioFormat() : null
                                    )
                            )
                    ) : info.bestVideoWithAudioFormat());
            if(format == null) {
                System.out.printf(ConsoleColors.RED+"Error: Unknown format `%s`%n"+ConsoleColors.RESET, args[1]);
                return;
            }
            ProgressBar pb1 = new ProgressBar("Downloading", 100);
            RequestVideoFileDownload request = new RequestVideoFileDownload(format)
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
                            System.out.printf(ConsoleColors.GREEN+"Successfully downloaded `%s`!%n"+ConsoleColors.RESET, title);
                            System.out.printf(ConsoleColors.YELLOW+"File saved at '%s'!%n"+ConsoleColors.RESET, f.getAbsolutePath());
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            pb1.close();
                            System.out.println(ConsoleColors.RED+"Error: " + throwable.getLocalizedMessage()+ConsoleColors.RESET);
                        }
                    })
                    .async();
            Response<File> fr = downloader.downloadVideoFile(request);
            fr.data();
        }else {
            System.out.println(ConsoleColors.RED+"Unknown argument found."+ConsoleColors.RESET);
            System.out.println("Possible arguments: ");
            System.out.println();
            System.out.println("--version > Show version & information of program");
            System.out.println("--update  > Update this program");
            System.out.println("(url)     > Download youtube video");
        }
    }
    public static VideoInfo getVideoInfo(String videoId){
        RequestVideoInfo request = new RequestVideoInfo(videoId)
                .callback(new YoutubeCallback<>() {
                    @Override
                    public void onFinished(VideoInfo videoInfo) {
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println("Error: " + throwable.getMessage());
                    }
                })
                .async();
        Response<VideoInfo> response = downloader.getVideoInfo(request);
        return response.data();
    }

    private static final String verUrl = "https://raw.githubusercontent.com/Yellowstrawberrys/Youtube-CLI/main/version.json";
    private static final String upUrl = "https://github.com/Yellowstrawberrys/Youtube-CLI/releases/latest/download/";

    private static void versionCheck() {
        if(!getLatestVersion().equals("nointernet") && !getLatestVersion().equals(v)) {
            System.out.printf(ConsoleColors.RED+"Your Youtube-CLI is outdated! (Current: %s, Latest: %s)%n", v, "1");
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
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
            try(BufferedInputStream ipt = new BufferedInputStream(con.getInputStream()); ProgressBar progressBar = new ProgressBar("Updating", ipt.available()); FileOutputStream os = new FileOutputStream("youtube-cli%s.part".formatted(isw?".exe":""), false)) {
                progressBar.setExtraMessage("youtube-cli%s.part".formatted(isw?".exe":""));

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
                        new ProcessBuilder("cmd", "/c", "cd %s && ping 127.0.0.1 -n 1 -w 500> nul && del /F /Q youtube-cli.exe && rename youtube-cli.exe.part youtube-cli.exe".formatted(f)).inheritIO().start();
                    }else {
                        new ProcessBuilder("/bin/bash", "-c", "cd %s && sleep .5 && rm -f youtube-cli && mv youtube-cli.part youtube-cli".formatted(f)).inheritIO().start();
                    }
                    System.out.println(ConsoleColors.GREEN+"Now, you're up-to-date!"+ConsoleColors.RESET);
                    System.exit(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String getLatestVersion() {
        try {
            return JsonReader.readFromUrl(verUrl).getString("version");
        } catch (Exception e) {
//            throw new RuntimeException(e);
            return "nointernet";
        }
    }
}
