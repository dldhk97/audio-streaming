package server;

import javax.sound.sampled.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {

    ArrayList<DataOutputStream> listeners;
    ServerSocket serverSocket;
    Socket listener;
    DataOutputStream dos;
    Server() {
        listeners = new ArrayList<>();
    }

    private void start() {
        try {
            serverSocket = new ServerSocket(10001);
            System.out.println("Server Started");
            new broadCast().start();

            while (true) {
                listener = serverSocket.accept();
                dos = new DataOutputStream(listener.getOutputStream());
                listeners.add(dos);
                System.out.println("Connected from [" + listener.getPort() + " : " + listener.getInetAddress() + "]");
                System.out.println("Current listener : " + listeners.size());
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }//start()

    public static void main(String[] args) {
        new Server().start();
    }//main()

    class broadCast extends Thread{
        TargetDataLine targetDataLine;
        SourceDataLine sourceDataLine;
        DataOutputStream lstn;

        final Boolean MICROPHONE_MODE = false;                                       // 마이크 모드인지, 파일 모드인지 설정
        final String WAV_FILE_PATH = "C:\\Users\\Administrator\\Desktop\\test.wav";   // wav 파일 절대경로

        @Override
        public void run() {
            int dsize = 0;
            AudioFormat format = null;
            DataLine.Info info;
            byte[] data = new byte[1024];

            AudioInputStream audioInputStream = null;

            try {
                if(MICROPHONE_MODE){
                    // use microphone
                    format = new AudioFormat(192000.0f, 16, 2, true, false);
                    targetDataLine = AudioSystem.getTargetDataLine(format);

                    info = new DataLine.Info(TargetDataLine.class, format);
                    targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
                    targetDataLine.open(format);
                    targetDataLine.start();
                }
                else{
                    // use mp3 file
                    File file = new File((WAV_FILE_PATH));
                    audioInputStream = AudioSystem.getAudioInputStream(file);

                    format = audioInputStream.getFormat();
                    info = new DataLine.Info(SourceDataLine.class, format);

                    sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
                    sourceDataLine.open(format);
                    sourceDataLine.start();
                }


            } catch(Exception e) {
                e.printStackTrace();
            }

            long testCnt = 1;
            while (true) {
                try {
                    if(MICROPHONE_MODE){
                        dsize = targetDataLine.read(data, 0, 1024);
                    }
                    else{
                        // 인텔리J 버그인지 콘솔 출력 안시키면 소리 안나올 때가 있음.
                        if(testCnt++ % 1000000000 == 0){
                            testCnt = 1;
                            System.out.println("Current Listener : " + listeners.size());
                        }

                        // 접속자가 있으면, 버퍼를 모두 비울 때까지 read.
                        if(listeners.size() > 0){
                            if(dsize != -1){
                                dsize = audioInputStream.read(data, 0, data.length);
                            }
                            else{
                                // 현 버퍼가 비어있으면 재생하지 않음.
                                sourceDataLine.drain();
                                sourceDataLine.close();
                                continue;
                            }
                        }
                    }

                    // 접속자 확인, 있으면 접속자 수만큼 버퍼 전송
                    for (int i = 0; i < listeners.size(); i++) {
                        lstn = listeners.get(i);
                        lstn.write(data, 0, dsize);
                    }
                } catch(Exception e) {
                    try {
                        // 접속자 연결 끊김 시
                        lstn.close();
                        listeners.remove(lstn);
                        System.out.println("Someone Disconnected");
                        System.out.println("Current listener : " + listeners.size());
                    } catch(IOException f) {
                        f.printStackTrace();
                    }
                }
            }
        }

        // 서버 로컬로 파일 재생
        private void playLocal(AudioInputStream audioInputStream) throws Exception{
            int nBytesRead = 0;
            byte[] data = new byte[1024];
            while(nBytesRead != -1){
                nBytesRead = audioInputStream.read(data, 0, data.length);
                if(nBytesRead >= 0){
                    int nBytesWritten = sourceDataLine.write(data,0,nBytesRead);
                }
            }

            sourceDataLine.drain();
            sourceDataLine.close();
        }
    }

}// server.Server class