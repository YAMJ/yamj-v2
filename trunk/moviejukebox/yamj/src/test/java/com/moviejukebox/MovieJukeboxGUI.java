package com.moviejukebox;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.xml.bind.JAXBContext;

import com.moviejukebox.MovieJukebox.JukeboxXml;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.MediaLibraryPath;
import com.moviejukebox.model.Movie;
import com.moviejukebox.scanner.MovieDirectoryScanner;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;

public class MovieJukeboxGUI implements Runnable {

String movieLibraryRoot = null;
String jukeboxRoot = "Jukebox";

private static Logger logger = Logger.getLogger("moviejukebox");

private static String readAll(InputStream is) throws IOException {
InputStreamReader r = new InputStreamReader(is, "UTF-8");
StringBuilder s = new StringBuilder();
char[] b = new char[1024];
for (;;) {
int n = r.read(b);
if (n < 0) break;
s.append(b, 0, n);
}
return s.toString();
}

private static void help() {
final JFrame root = new JFrame("YAMJ GUI Help");

root.setLayout(new BorderLayout());
String text;
try {
text = readAll(MovieJukeboxGUI.class
.getResourceAsStream("MovieJukeboxGUIHelp.txt"));
} catch (IOException e) {
text = "Cannot find help";
}
JTextArea textArea = new JTextArea(text, 30, 80);
textArea.setEditable(false);
root.add(new JScrollPane(textArea), BorderLayout.CENTER);

root.pack();
root.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
root.setVisible(true);
}

private MovieJukeboxGUI(String[] args) throws ClassNotFoundException,
InstantiationException, IllegalAccessException,
UnsupportedLookAndFeelException {

Map<String, String> cmdLineProps = new LinkedHashMap<String, String>();

for (int i = 0; i < args.length; i++) {
String arg = (String) args[i];
if ("-v".equalsIgnoreCase(arg)) {
// We've printed the version, so quit now
return;
} else if ("-o".equalsIgnoreCase(arg)) {
jukeboxRoot = args[++i];
} else if ("-c".equalsIgnoreCase(arg)) {
// MovieJukebox.jukeboxClean = true;
} else if ("-k".equalsIgnoreCase(arg)) {
MovieJukebox.setJukeboxPreserve(true);
} else if ("-p".equalsIgnoreCase(arg)) {
PropertiesUtil.setPropertiesStreamName(args[++i]);
} else if ("-i".equalsIgnoreCase(arg)) {
// MovieJukebox.skipIndexGeneration = true;
} else if ("-dump".equalsIgnoreCase(arg)) {
// MovieJukebox.dumpLibraryStructure = true;
} else if (arg.startsWith("-D")) {
String propLine = arg.length() > 2 ? arg.substring(2)
: args[++i];
int propDiv = propLine.indexOf("=");
if (-1 != propDiv) {
cmdLineProps.put(propLine.substring(0, propDiv),
propLine.substring(propDiv + 1));
}
} else if (arg.startsWith("-")) {
help();
return;
} else {
movieLibraryRoot = args[i];
}
}

jukeboxRoot = FileTools.getCanonicalPath(jukeboxRoot);
        movieLibraryRoot = FileTools.getCanonicalPath(movieLibraryRoot); 
}

public void run() {

final JFrame root = new JFrame("YAMJ GUI");
root.setLayout(new BorderLayout());
JPanel params = new JPanel(new GridBagLayout());
root.add(params, BorderLayout.NORTH);
params.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

final GridBagConstraints lc = new GridBagConstraints();
final GridBagConstraints ec = new GridBagConstraints();

lc.fill = GridBagConstraints.NONE;
lc.gridx = 0;
lc.gridy = 0;
lc.anchor = GridBagConstraints.WEST;
lc.insets = new Insets(0, 0, 0, 5);
params.add(new JLabel("Library path:"), lc);

ec.fill = GridBagConstraints.HORIZONTAL;
ec.weightx = 1;
ec.gridx = 1;
ec.gridy = 0;
params.add(new JTextField(movieLibraryRoot), ec);

lc.gridy ++;
params.add(new JLabel("Jukebox output path:"), lc);

ec.gridy ++;
params.add(new JTextField(jukeboxRoot), ec);

final JTextArea logTextArea = new JTextArea("", 10, 80);
logTextArea.setEditable(false);
JScrollPane sp = new JScrollPane(logTextArea);
sp.setBorder(new TitledBorder("Log"));
root.add(sp, BorderLayout.SOUTH);

logger.addHandler(new Handler() {
@Override
public void publish(LogRecord record) {
logTextArea.append(record.getMessage() + "\r\n");
if (record.getParameters() != null) {
for (Object param : record.getParameters()) {
logTextArea.append("- " + param.toString() + "\r\n");
}
}
if (record.getThrown() != null) {
logTextArea.append(" " + record.getThrown().toString() + "\r\n");
for (StackTraceElement el : record.getThrown().getStackTrace()) {
logTextArea.append("- " + el.toString() + "\r\n");
}
}
final int l=logTextArea.getDocument().getLength();
                
// Make sure the last line is always visible
logTextArea.setCaretPosition(l);
}

@Override
public void flush() {
}

@Override
public void close() throws SecurityException {
}
});


final JTable oldMoviesTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(oldMoviesTable);
        scrollPane.setPreferredSize(new Dimension(100,200));
        //scrollPane.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Edit"));
        root.add(scrollPane, BorderLayout.CENTER);

root.pack();
root.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
root.setVisible(true);

logger.info("Loading old movies");

        try {
JAXBContext context = JAXBContext.newInstance(JukeboxXml.class);
final JukeboxXml jukeboxXml = (JukeboxXml) context.createUnmarshaller().unmarshal(new File(jukeboxRoot, "Jukebox/CompleteMovies.xml"));
logger.info("Movies loaded: " + jukeboxXml.movies.size());
oldMoviesTable.setModel(new AbstractTableModel() {

                private static final long serialVersionUID = 1L; // Default serial UID

                @Override
public String getColumnName(int column) {
return properties.get(column);
}

private final List<String> properties = new ArrayList<String>(){
                    private static final long serialVersionUID = 1L; // Default serial UID

                    {
add("getTitle");
add("getBaseFilename");
add("getPlot");
add("getResolution");
add("getPosterFilename");
}
};

@Override
public Object getValueAt(int row, int col) {

try {
return Movie.class.getMethod(properties.get(col)).invoke(jukeboxXml.movies.get(row));
} catch (Exception e) {
return "Error";
}
}

@Override
public int getRowCount() {
return jukeboxXml.movies.size();
}

@Override
public int getColumnCount() {
// TODO Auto-generated method stub
return properties.size();
}
});


            MovieDirectoryScanner mds = new MovieDirectoryScanner();
            // scan uses synchronized method Library.addMovie
            
            Library library = new Library();
            MediaLibraryPath mlp = new MediaLibraryPath();
            mlp.setPath(movieLibraryRoot);
            mlp.setPlayerRootPath("test");
            mlp.setScrapeLibrary(true);
            mlp.setExcludes(new ArrayList<String>());
            
            mds.scan(mlp, library);


} catch (Exception e) {
logger.log(Level.SEVERE, "Cannot continue processing", e);
}

}

/**
 * @param args
 * @throws UnsupportedLookAndFeelException
 * @throws IllegalAccessException
 * @throws InstantiationException
 * @throws ClassNotFoundException
 */
public static void main(String[] args) throws ClassNotFoundException,
InstantiationException, IllegalAccessException,
UnsupportedLookAndFeelException {
UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

if (args.length == 0) {
help();
return;
}

try {

new MovieJukeboxGUI(args).run();

} catch (Exception error) {
logger.log(Level.SEVERE, "Cannot continue processing", error);
help();
return;
}
}

}
