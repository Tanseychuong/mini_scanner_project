import org.w3c.dom.ls.LSOutput;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

class NetworkPacketScanner{
    String destination;
    static void main(String[] args){
        Scanner input = new Scanner(System.in);
        System.out.println("Enter the  process you want to scanner");
        destination = input.next();
    }
    private void Netstat(){
    Process process1 = Runtime.getRuntime().exec("route add "+destination+" mask "+[subnet_mask]+" "+[gateway]);
    BufferedReader reader1 = new BufferedReader(new InputStreamReader(process1.getInputStream()));
}}