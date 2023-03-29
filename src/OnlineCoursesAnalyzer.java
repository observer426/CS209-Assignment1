import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is just a demo for you, please run it on JDK17 (some statements may be not allowed in
 * lower version).
 * This is just a demo, and you can extend and implement functions
 * based on this demo, or implement it in a different way.
 */
public class OnlineCoursesAnalyzer {
  List<Course> courses = new ArrayList<>();

  public OnlineCoursesAnalyzer(String datasetPath) {
    BufferedReader br = null;
    String line;
    try {
      br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
      br.readLine();
      while ((line = br.readLine()) != null) {
        String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
        Course course = new Course(info[0], info[1], new Date(info[2]), info[3], info[4], info[5],
            Integer.parseInt(info[6]), Integer.parseInt(info[7]), Integer.parseInt(info[8]),
            Integer.parseInt(info[9]), Integer.parseInt(info[10]),
            Double.parseDouble(info[11]), Double.parseDouble(info[12]),
            Double.parseDouble(info[13]), Double.parseDouble(info[14]),
            Double.parseDouble(info[15]), Double.parseDouble(info[16]),
            Double.parseDouble(info[17]), Double.parseDouble(info[18]),
            Double.parseDouble(info[19]), Double.parseDouble(info[20]),
            Double.parseDouble(info[21]), Double.parseDouble(info[22]));
        courses.add(course);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  //1
  public Map<String, Integer> getPtcpCountByInst() {
    Map<String, Integer> insCnt = courses.stream()
        .collect(Collectors.groupingBy(Course::getInstitution,
                Collectors.summingInt(Course::getParticipants)));
    Map<String, Integer> sorted = insCnt.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .collect(
        Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
          (oldVal, newVal) -> oldVal,
        LinkedHashMap::new)
      );
    return sorted;
  }

  //2
  public Map<String, Integer> getPtcpCountByInstAndSubject() {
    Map<String, Integer> init = courses.stream()
        .collect(
        Collectors.groupingBy(
          course -> course.institution + "-" + course.subject,
        Collectors.summingInt(Course::getParticipants)
      )
      );
    Map<String, Integer> sorted = init.entrySet().stream()
      .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
      .collect(
      Collectors.toMap(
      Map.Entry::getKey,
      Map.Entry::getValue,
      (oldVal, newVal) -> oldVal,
      LinkedHashMap::new)
      );
    return sorted;
  }

  //3
  public Map<String, List<List<String>>> getCourseListOfInstructor() {
        List<String> instr = new ArrayList<>();
        List<String> instrStr = courses.stream()
      .map(course -> course.instructors)
      .distinct()
      .toList();
    instrStr.forEach(
      s -> {
    instr.addAll(Arrays.asList(s.split(", ")));
      }
    );
        List<String> finInstructor = instr.stream().distinct().toList();
    Map<String, List<List<String>>> courseList = new HashMap<>();
    finInstructor.forEach(
      s -> {
        List<String> independent = courses.stream()
    .filter(course -> course.instructors.equals(s))
    .map(Course::getTitle)
    .sorted()
    .distinct().toList();
        List<String> dependent = courses.stream()
    .filter(course -> !course.instructors.equals(s) &&
      Arrays.stream(course.instructors.split(", ")).toList().contains(s)
    )
    .map(Course::getTitle)
    .sorted()
    .distinct().toList();
        List<List<String>> total = new ArrayList<>();
        total.add(independent);
        total.add(dependent);
        courseList.put(s, total);
      }
    );
    return courseList;
  }

  //4
  public List<String> getCourses(int topK, String by) {
    List<String> topCourse = new ArrayList<>();
    if (by.equals("hours")) {
    topCourse = courses.stream()
    .sorted(
    Comparator.comparing(Course::getTotalHours).reversed()
      .thenComparing(Course::getTitle)
    ).map(Course::getTitle)
    .distinct()
    .limit(topK)
    .collect(Collectors.toList());
    } else if (by.equals("participants")) {
    topCourse = courses.stream()
    .sorted(
    Comparator.comparing(Course::getParticipants).reversed()
      .thenComparing(Course::getTitle)
    ).map(Course::getTitle)
    .distinct()
    .limit(topK)
    .collect(Collectors.toList());
    }
    return topCourse;
  }

  //5
  public List<String> searchCourses(String courseSubject, double percentAudited, double totalCourseHours) {
    List<String> selectCourses = courses.stream()
      .filter(
      course -> (
      course.subject.matches("(.*)" + "(?i)" + courseSubject + "(.*)")
        && course.percentAudited >= percentAudited
        && course.totalHours <= totalCourseHours
      )
      ).map(Course::getTitle)
      .distinct()
      .sorted()
      .toList();
    return selectCourses;
  }

  //6
  public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {

    Map<String, Double> avgAge = courses.stream()
      .collect(
      Collectors.groupingBy(Course::getNumber,
      Collectors.averagingDouble(Course::getMedianAge)
      )
      );
    Map<String, Double> avgMale = courses.stream()
      .collect(
      Collectors.groupingBy(Course::getNumber,
      Collectors.averagingDouble(Course::getPercentMale)
      )
      );
    Map<String, Double> avgDegree = courses.stream()
      .collect(
      Collectors.groupingBy(Course::getNumber,
      Collectors.averagingDouble(Course::getPercentDegree)
      )
      );
    Map<String, Double> similarity = new HashMap<>();
    avgAge.forEach(
      (s, aDouble) -> {
    double simi = Math.pow(age - aDouble, 2) + Math.pow(gender * 100 - avgMale.get(s), 2)
    + Math.pow(isBachelorOrHigher * 100 - avgDegree.get(s), 2);
    similarity.put(s, simi);
      }
    );
    Map<String, String> titles = new HashMap<>();
    similarity.forEach(
      (s, aDouble) -> {
    titles.put(s,
    courses.stream()
      .filter(course -> course.number.equals(s))
      .max(Comparator.comparing(Course::getLaunchDate)).get().title);
      }
    );

    List<Q6> fin = new ArrayList<>();
    similarity.forEach(
      (s, aDouble) -> {
    fin.add(new Q6(s, titles.get(s), aDouble));
      }
    );
    List<Q6> sorted = fin.stream()
      .sorted(Comparator.comparing(Q6::getSimi).thenComparing(Comparator.comparing(Q6::getTitle)))
      .toList();
//    sorted.forEach(
//      s->{
//    System.out.println(s.title + " " +s.simi);
//      }
//    );
    List<String> finalTitle = sorted.stream()
      .map(Q6::getTitle)
      .distinct().limit(10).toList();
    return finalTitle;
  }

}

class Q6 {
  String number;
  String title;
  double simi;

  public Q6(String number, String title, double simi) {
    this.number = number;
    this.title = title;
    this.simi = simi;
  }

  public String getTitle() {
    return title;
  }

  public double getSimi() {
    return simi;
  }
}

class Course {
  String institution;
  String number;
  Date launchDate;
  String title;
  String instructors;
  String subject;
  int year;
  int honorCode;
  int participants;
  int audited;
  int certified;
  double percentAudited;
  double percentCertified;
  double percentCertified50;
  double percentVideo;
  double percentForum;
  double gradeHigherZero;
  double totalHours;
  double medianHoursCertification;
  double medianAge;
  double percentMale;
  double percentFemale;
  double percentDegree;

  public Course(String institution, String number, Date launchDate,
      String title, String instructors, String subject,
      int year, int honorCode, int participants,
      int audited, int certified, double percentAudited,
      double percentCertified, double percentCertified50,
      double percentVideo, double percentForum, double gradeHigherZero,
      double totalHours, double medianHoursCertification,
      double medianAge, double percentMale, double percentFemale,
      double percentDegree) {
    this.institution = institution;
    this.number = number;
    this.launchDate = launchDate;
    if (title.startsWith("\"")) title = title.substring(1);
    if (title.endsWith("\"")) title = title.substring(0, title.length() - 1);
    this.title = title;
    if (instructors.startsWith("\"")) instructors = instructors.substring(1);
    if (instructors.endsWith("\"")) instructors = instructors.substring(0, instructors.length() - 1);
    this.instructors = instructors;
    if (subject.startsWith("\"")) subject = subject.substring(1);
    if (subject.endsWith("\"")) subject = subject.substring(0, subject.length() - 1);
    this.subject = subject;
    this.year = year;
    this.honorCode = honorCode;
    this.participants = participants;
    this.audited = audited;
    this.certified = certified;
    this.percentAudited = percentAudited;
    this.percentCertified = percentCertified;
    this.percentCertified50 = percentCertified50;
    this.percentVideo = percentVideo;
    this.percentForum = percentForum;
    this.gradeHigherZero = gradeHigherZero;
    this.totalHours = totalHours;
    this.medianHoursCertification = medianHoursCertification;
    this.medianAge = medianAge;
    this.percentMale = percentMale;
    this.percentFemale = percentFemale;
    this.percentDegree = percentDegree;
  }

  public String getInstitution() {
    return institution;
  }

  public int getParticipants() {
    return participants;
  }

  public String getInstructors() {
    return instructors;
  }

  public double getTotalHours() {
    return totalHours;
  }

  public String getTitle() {
    return title;
  }

  public String getSubject() {
    return subject;
  }

  public double getMedianAge() {
    return medianAge;
  }

  public double getPercentMale() {
    return percentMale;
  }

  public double getPercentDegree() {
    return percentDegree;
  }

  public String getNumber() {
    return number;
  }

  public Date getLaunchDate() {
    return launchDate;
  }
}