import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StatsManager {
    private Map<String, Value> map;
    private String fileName;

    public StatsManager(String fileName)
    {
        System.out.println("StatusManager initialized");
        System.out.println(fileName);
        this.fileName = fileName;
        this.map = Collections.synchronizedMap(new HashMap<>());
    }

    public void readFromFile() {
        System.out.println("Read from file started");
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(this.fileName))) {
            while ((line = br.readLine()) != null) {
                String [] fields = line.split(";");
                String domain = fields[0];
                int num = Integer.parseInt(fields[1]);
                int size = Integer.parseInt(fields[2]);
                map.put(domain, new Value(num,size));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Read from stats finished");
    }

    public synchronized void writeToFile()
    {
        String line;
        try (BufferedWriter wr = new BufferedWriter(new FileWriter(this.fileName))) {
            for (String key : this.map.keySet()) {
                line = key + ";" + this.map.get(key).getNum() + ";" + this.map.get(key).getSize() + "\n";
                wr.write(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public synchronized void update(String key, int size)
    {
        if(this.map.containsKey(key)) {
            int num = this.map.get(key).getNum() + 1;
            size += this.map.get(key).getSize();
            this.map.put(key, new Value(num, size));
        } else {
            this.map.put(key, new Value(1, size));
        }
    }

    @Override
    public String toString()
    {
        String output = "";
        for (String key : this.map.keySet()) {
            output += key + ";" + this.map.get(key).getNum() + ";" + this.map.get(key).getSize() + "\n";
        }
        if (output.equals(""))
            output = "Empty stats";

        return output;
    }
}

class Value {
    private int num;
    private int size;

    public Value(int num, int size){
        this.num  = num;
        this.size = size;
    };

    public int getSize() { return this.size;}
    public int getNum() {return this.num;}
    public void setSize (int size) { this.size = size;}
    public void setNum (int num) { this.num = num;}

    @Override
    public String toString() {
        return "{ num:" + this.num + ", size:" + this.size + "}";
    }
}

