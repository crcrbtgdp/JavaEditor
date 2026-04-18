import java.awt.*;
import java.awt.datatransfer.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.TreeMap;
import javax.swing.*;

public class TextEditor {

    static JTextArea textArea;
    static JTextArea textArea2;
    static Clipboard clip;
    static StringSelection str;
    static File file;
    static File fileChecker;
    static String path;
    static String path2;
    static boolean get;
    static String time;
    static int width;
    static int height;
    static File jsonFile;//これ多分要らない
    static TreeMap<Long, String> q;
    static boolean cameFromTry;
    static JMenuItem setPath;

    public static void main(String[] args) {

        q = new TreeMap<>();
        cameFromTry = false;

        get = true;
        clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        //フレームを作る
        JFrame frame = new JFrame("自作テキストエディタ");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        width = 900;
        height = 600;
        frame.setSize(width, height);

        JMenuBar bar = new JMenuBar();

        JMenu menuFile = new JMenu("ファイル");
        JMenu menuRun = new JMenu("実行");

        JMenuItem run = new JMenuItem("前のコンパイルを実行");
        JMenuItem compile = new JMenuItem("コンパイルして実行");
        //内部クラスで使うためstaticフィールドにした
        setPath = new JMenuItem("保存先を選択");
        JMenuItem clear = new JMenuItem("コンソールをクリア");
        JMenuItem copy = new JMenuItem("コピー");
        JMenuItem paste = new JMenuItem("ペースト");

        //設定ファイル読み込みプログラムの定義
        class ReadConfig {
            public static boolean read() {
                boolean ret;
                try {
                    File pathReader = new File(System.getProperty("user.dir") + File.separator + "config.txt");
                    if (!pathReader.exists()) {throw new IOException("ファイル置き場を設定する必要があります！");}
                    FileReader fr = new FileReader(pathReader);
                    path2 = fr.readAllAsString();

                    q.clear();
                    File fi1 = new File(path2);
                    File[] fi2 = fi1.listFiles();
                    if(fi2 != null && fi2.length > 0) {
                        for(File fi3 : fi2) {
                            String fi4 = fi3.getName();
                            q.put(fi3.lastModified(), fi4.substring(0, fi4.lastIndexOf(".")));
                        }
                        time = q.lastEntry().getValue();
                        path = path2 + File.separator + time;
                    }
                    ret = true;
                } catch(NullPointerException | IOException e) {
                    textArea2.setText(e.getMessage());
                    ret = false;
                }
                return ret;
            }
        }
        
        //前のコンパイルを実行 ボタンが押されたとき
        run.addActionListener(e -> {
            if(path != null && !path.isEmpty()) {
                ProcessBuilder pb = null;
                Process p = null;
                //共通プログラムなので別メソッドで処理
                p = process(pb, p, false);
                try {
                    if(get) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), "MS932"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        textArea2.setText(sb.toString());
                    } else {textArea2.setText("コンパイル済みのファイルがありません！"); get = true;}
                } catch(IOException ex) {
                    textArea2.setText("エディタ側のエラー：" + ex.toString());
                }
            } else {textArea2.setText("コンパイル済みのファイルがありません！");}
        });

        //コンパイルして実行 ボタンが押されたとき
        compile.addActionListener(e -> {
            try {
                time = "J" + String.valueOf(System.currentTimeMillis());
                path = path2 + File.separator + time;
                file = new File(path + ".java");
                file.createNewFile();
                FileWriter fw = new FileWriter(file);
                fw.write(textArea.getText().replace("Main", time));
                fw.close();

                //コンパイル
                ProcessBuilder pb = new ProcessBuilder("javac", path + ".java", "-encoding", "UTF-8");
                Process p = pb.start();
                p.waitFor();

                //共通プログラムなので別メソッドで処理
                p = process(pb, p, true);
                
                //出力・エラーを表示
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), "MS932"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                textArea2.setText(sb.toString());

            } catch (IOException | InterruptedException ex) {
                textArea2.setText("エディタ側のエラー：" + ex.toString());
            }
        });

        //保存先を選択 ボタンが押されたとき
        setPath.addActionListener(e -> {
            try {
                JFileChooser filechooser = new JFileChooser();
                filechooser.setDialogTitle("コンパイル先を選択...");
                filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int selected = filechooser.showOpenDialog(frame);
                if (selected == JFileChooser.APPROVE_OPTION){
                    file = new File(System.getProperty("user.dir") + File.separator + "config.txt");
                    boolean exists = file.createNewFile();
                    if (!exists && file.exists()) {
                        textArea2.setText("すでに設定されています");
                    } else if (!exists) {
                        textArea2.setText("失敗しました");
                    } else {
                        textArea2.setText("設定しました");
                        path2 = filechooser.getSelectedFile().getPath();
                        FileWriter fw = new FileWriter(file);
                        fw.write(path2);
                        fw.close();
                    }
                }
            } catch(IOException ex) {
                textArea2.setText("エディタ側のエラー：" + ex.toString());
            }
        });

        //コンソールをクリア ボタンが押されたとき
        clear.addActionListener(e -> {
            textArea2.setText("");
        });

        //コピー ボタンが押されたとき
        copy.addActionListener(e -> {
            str = new StringSelection(textArea.getText());
            clip.setContents(str, null);
        });

        //ペースト ボタンが押されたとき
        paste.addActionListener(e -> {
            Transferable trans = clip.getContents(null);
            if (trans != null && trans.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    textArea.setText((String) trans.getTransferData(DataFlavor.stringFlavor));
                } catch(UnsupportedFlavorException | IOException ex) {}
            }
        });

        menuRun.add(compile);
        menuRun.add(run);
        menuRun.add(clear);
        menuRun.add(setPath);
        menuFile.add(copy);
        menuFile.add(paste);

        bar.add(menuFile);
        bar.add(menuRun);

        frame.setJMenuBar(bar);

        compile.setActionCommand("compile");
        copy.setActionCommand("copy");
        clear.setActionCommand("clear");
        setPath.setActionCommand("setPath");
        paste.setActionCommand("paste");
        run.setActionCommand("run");


        //テキストエリアを作る
        textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));

        textArea2 = new JTextArea();
        textArea2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        textArea2.setLineWrap(true);
        frame.add(textArea2, BorderLayout.SOUTH);

        
        //スクロールできるようにする
        JScrollPane scrollPane = new JScrollPane(textArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        //設定ファイルの作成または読み込み
        if (!ReadConfig.read()) {setPath.doClick();}
        if (!ReadConfig.read()) {textArea2.setText("フォルダの読み込みに失敗しました。前回起動時にコンパイルしたファイルは［前のコンパイルを実行］ボタンで開くことはできません。");}

        //表示する
        frame.setVisible(true);
    }

    public static Process process(ProcessBuilder a, Process b, boolean c) {
        fileChecker = new File(path + ".class");
        if(fileChecker.exists()){/*検査例外起きたか確認*/
            //実行
            a = new ProcessBuilder("java", String.valueOf(time));
            a.directory(new File(path2));
            a.redirectErrorStream(true);
            try {
                b = a.start();
                
                //標準入力
                try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(b.getOutputStream(), "UTF-8"))) {
                    writer.write(textArea2.getText());
                    writer.flush();
                }
                

                b.waitFor();
            } catch (IOException | InterruptedException ex) {
                textArea2.setText("エディタ側のエラー：" + ex.toString());
            }   
        } else if(!c) {
            get = false;
        }
        return b;
    }
}
