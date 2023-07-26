import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PokerGame {

    private static final byte[] CF_CARDS = Bytes.toBytes("cards");
    private static final byte[] QUALIFIER_SUIT = Bytes.toBytes("suit");
    private static final byte[] QUALIFIER_VALUE = Bytes.toBytes("value");

    public static class PokerGameMapper extends TableMapper<Text, IntWritable> {

        public void map(ImmutableBytesWritable row, Result value, Context context)
                throws IOException, InterruptedException {
            String suit = Bytes.toString(value.getValue(CF_CARDS, QUALIFIER_SUIT));
            String cardValue = Bytes.toString(value.getValue(CF_CARDS, QUALIFIER_VALUE));

            try {
                int cardIntValue = Integer.parseInt(cardValue);
                context.write(new Text(suit), new IntWritable(cardIntValue));
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse card value: " + cardValue + " for suit: " + suit);
            }
        }
    }

    public static class PokerGameReducer extends Reducer<Text, IntWritable, Text, IntWritable> {

        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            context.write(key, new IntWritable(sum));
        }
    }

    public static void createAndPopulateTable(String tableName, String columnFamily) throws IOException {
        Configuration config = HBaseConfiguration.create();

        try (Connection connection = ConnectionFactory.createConnection(config);
             Admin admin = connection.getAdmin();
             Table hTable = connection.getTable(TableName.valueOf(tableName))) {

            if (!admin.tableExists(TableName.valueOf(tableName))) {
                TableDescriptor descriptor = TableDescriptorBuilder.newBuilder(TableName.valueOf(tableName))
                        .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamily)).build())
                        .build();
                admin.createTable(descriptor);
            }

            List<String> deck = generateDeck();

            for (int i = 0; i < deck.size(); i++) {
                String[] card = deck.get(i).split(" of ");
                Put p = new Put(Bytes.toBytes("row" + i));

                p.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes("suit"), Bytes.toBytes(card[1]));
                p.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes("value"), Bytes.toBytes(card[0]));
                hTable.put(p);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String tableName = "CardTable";
        String columnFamily = "cards";

        createAndPopulateTable(tableName, columnFamily);

        List<String> deck = generateDeck();
        System.out.println("Generated Deck: " + deck);

        Configuration conf = HBaseConfiguration.create();
        Scan scan = new Scan();
        scan.addFamily(CF_CARDS);

        Job job = Job.getInstance(conf, "PokerGame");
        job.setJarByClass(PokerGame.class);

        TableMapReduceUtil.initTableMapperJob(
                tableName,
                scan,
                PokerGameMapper.class,
                Text.class,
                IntWritable.class,
                job);

        job.setReducerClass(PokerGameReducer.class);
        FileOutputFormat.setOutputPath(job, new Path(args[0]));

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

    public static List<String> generateDeck() {
        String[] suits = {"hearts", "diamonds", "clubs", "spades"};
        String[] ranks = {
                "2", "3", "4", "5", "6", "7", "8", "9", "10", "jack", "queen", "king", "ace"
        };

        List<String> deck = new ArrayList<>();
        for (String suit : suits) {
            for (String rank : ranks) {
                deck.add(rank + " of " + suit);
            }
        }
        return deck;
    }
}

