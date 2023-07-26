import java.util.PriorityQueue;
import java.io.IOException;
import java.util.PriorityQueue;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class RelativeFrequency {
    public static class TokenizerMapper extends Mapper<Object, Text, NullWritable, Text> {
        private PriorityQueue<WordPair> queue;

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            queue = new PriorityQueue<>();
        }

    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String[] tokens = value.toString().split("\\s+");
            String wordA = tokens[0];
            String wordB = tokens[1];
            double relativeFrequency = Double.parseDouble(tokens[2]);
            queue.add(new WordPair(relativeFrequency, wordA, wordB));

            // keep only top 100
        if (queue.size() > 100) {
            queue.poll();
        }
        }

    @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            while (!queue.isEmpty()) {
                WordPair wp = queue.poll();
                context.write(NullWritable.get(), new Text(wp.toString()));
            }
        }
    }

    public static class TopNReducer extends Reducer<NullWritable, Text, NullWritable, Text> {
        private PriorityQueue<WordPair> queue;

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            queue = new PriorityQueue<>();
        }

    public void reduce(NullWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            for (Text value : values) {
                String[] tokens = value.toString().split("\\s+");
                String wordA = tokens[0];
                String wordB = tokens[1];
                double relativeFrequency = Double.parseDouble(tokens[2]);
                queue.add(new WordPair(relativeFrequency, wordA, wordB));

                // keep only top 100
                if (queue.size() > 100) {
                    queue.poll();
                }
            }

        while (!queue.isEmpty()) {
            WordPair wp = queue.poll();
            context.write(NullWritable.get(), new Text(wp.toString()));
        }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "relative frequency");
        job.setJarByClass(RelativeFrequency.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setCombinerClass(TopNReducer.class);
        job.setReducerClass(TopNReducer.class);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
