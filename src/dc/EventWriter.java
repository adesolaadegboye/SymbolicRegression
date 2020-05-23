
package dc;

import static dc.EventWriter.Type.Downturn;
import static dc.EventWriter.Type.DownwardOvershoot;
import static dc.EventWriter.Type.Upturn;
import static dc.EventWriter.Type.UpwardOvershoot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;

public class EventWriter
{
    public static void main(String[] args)
        throws Exception
    {
        if (args.length < 2)
        {
            System.out.println("usage: " + EventWriter.class.getName()
                + " <file> <threshold> [date]");
            System.exit(1);
        }

        File input = new File(args[0]);

        if (!input.exists())
        {
            System.out.println("Invalid input file: " + args[0]);
            System.exit(1);
        }

        ArrayList<Event> events = new ArrayList<Event>();
        Type event = Type.Upturn;

        double delta = Double.parseDouble(args[1]);
        double pHigh = 0;
        double pLow = 0;

        int[] indexDC = new int[2];
        int[] indexOS = new int[2];
        int index = 1;

        Event last = null;
        BufferedReader reader = new BufferedReader(new FileReader(input));
        String line = null;

        while ((line = reader.readLine()) != null)
        {
            String[] tokens = line.split("\\s");
            double value = Double.parseDouble(tokens[2].trim());

            if (args.length < 3 || tokens[0].trim().equals(args[2]))
            {
                if (index == 1)
                {
                    // it is the first line
    
                    pHigh = value;
                    pLow = value;
    
                    Arrays.fill(indexDC, index);
                    Arrays.fill(indexOS, index);
                }
                else if (event == Type.Upturn)
                {
                    if (value <= (pHigh * (1 - delta)))
                    {
                        // overshoot event must have a start index lower that
                        // the DC event start index
                        if (indexOS[0] < indexOS[1] && indexOS[0] < indexDC[0])
                        {
                            last = new Event(indexOS[0], indexOS[1], UpwardOvershoot); 
                            // last known upward overshoot position
                            events.add(last);
                        }
                        
                        if (last != null)
                        {
                            // we might miss the start of an event
                            if (indexDC[0] == last.start)
                            {
                                indexDC[0] = last.end + 1;
                            }
                            // we might skip the start of an event when there
                            // are repeated values or large increases during an
                            // upturn overshoot followed by a downturn event and
                            // vice-versa (the overshoot will be invalid since
                            // the end index will be smaller than the start index)
                            else if (indexDC[0] > (last.end + 1))
                            {
                                indexDC[0] = (last.end + 1);
                            }
                        }
                        
                        // downturn detected
                        event = Downturn;
                        pLow = value;
    
                        indexDC[1] = index;
                        indexOS[0] = index + 1; // start of the overshoot
    
                        last = new Event(indexDC[0], indexDC[1], Downturn);
                        events.add(last);
                    }
                    else if (pHigh < value)
                    {
                        pHigh = value;
    
                        indexDC[0] = index;
                        indexOS[1] = index - 1;
                    }
                }
                else
                {
                    if (value >= (pLow * (1 + delta)))
                    {
                        // overshoot event must have a start index lower that
                        // the DC event start index
                        if (indexOS[0] < indexOS[1] && indexOS[0] < indexDC[0])
                        {
                            last = new Event(indexOS[0], indexOS[1], DownwardOvershoot);
                            // last known downward overshoot position
                            events.add(last);
                        }
                        
                        if (last != null)
                        {
                            // we might not have recorded the start index of
                            // the event
                            if (indexDC[0] == last.start)
                            {
                                indexDC[0] = last.end + 1;
                            }
                            else if (indexDC[0] > (last.end + 1))
                            {
                                indexDC[0] = (last.end + 1);
                            }
                        }
    
                        // upturn detected
                        event = Upturn;
                        pHigh = value;
    
                        indexDC[1] = index;
                        indexOS[0] = index + 1; // start of the overshoot
    
                        last = new Event(indexDC[0], indexDC[1], Upturn);
                        events.add(last);
                    }
                    else if (pLow > value)
                    {
                        pLow = value;
    
                        indexDC[0] = index;
                        indexOS[1] = index - 1;
                    }
                }
    
                index++;
            }
        }

        reader.close();

        // fix start index of events
/* 
        ArrayList<Event> reverse = new ArrayList<Event>(events);
        Collections.reverse(reverse);

        Event last = null;

        for (Event e : reverse)
        {
            if (last != null && e.start == last.start)
            {
                last.start = e.end + 1;
            }

            last = e;
        }
*/
        for (Event e : events)
        {
            System.out.println(e);
            if (e.overshoot != null)
            	System.out.println(e.overshoot);
        }
    }

    public enum Type
    {
        Upturn, Downturn, UpwardOvershoot, DownwardOvershoot;
    }

    public static class Event
    {
        public int start = 0;
        public int end = 0;
        public Type type;
        public Event overshoot;

        public Event(int start, int end, Type type)
        {
            this.start = start;
            this.end = end;
            this.type = type;
        }

        @Override
        public String toString()
        {
            return String.format("%4d %4d   %s", start, end, type);
        }
    }
}