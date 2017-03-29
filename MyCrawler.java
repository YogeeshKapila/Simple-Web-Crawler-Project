import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.http.HttpStatus;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.BinaryParseData;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

public class MyCrawler extends WebCrawler {
	private final static Pattern FILTERS = Pattern
			.compile(".*(\\.(css|xml|js|mp3|zip|gz|mid|mp2|mp4|wav|avi|mov|mpeg|ram|m4v|rm|smil|wmv|swf|wma|rar))$");

	public static int totalFetchesAttempted = 0;
	public static int fetchesSucceeded = 0;
	public static int fetchesFailed = 0;
	public static int fetchesAborted = 0;
	public static int totalUrlsExtracted = 0;

	public static Map<Integer,Integer> statusCodes = new HashMap<Integer, Integer>();
	public static Map<String, Integer> contentTypes = new HashMap<String, Integer>();
	public static Map<String, Integer> contentSizes = new HashMap<String, Integer>();

	public static Set<String> totalUniqueUrls = new HashSet<String>();
	public static Set<String> uniqueWithin = new HashSet<String>();
	public static Set<String> uniqueOutside = new HashSet<String>();

	/**
	 * This method receives two parameters. The first parameter is the page in
	 * which we have discovered this new url and the second parameter is the new
	 * url. You should implement this function to specify whether the given url
	 * should be crawled or not (based on your crawling logic). In this example,
	 * we are instructing the crawler to ignore urls that have css, js, git, ...
	 * extensions and to only accept urls that start with
	 * "http://www.viterbi.usc.edu/". In this case, we didn't need the
	 * referringPage parameter to make the decision.
	 */

	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {
		String href = url.getURL().toLowerCase();
		String indicator;
		totalUrlsExtracted++;
		totalUniqueUrls.add(href);

		if (href.contains("abcnews.go.com")) {
			indicator = "OK";
			uniqueWithin.add(href);
		} else {
			indicator = "N_OK";
			uniqueOutside.add(href);
		}

		PrintWriter pw;
		try {
			pw = new PrintWriter(new FileWriter("urls.csv", true));

			StringBuilder sb = new StringBuilder();
			sb.append(href);
			sb.append(',');
			sb.append(indicator);
			sb.append('\n');

			pw.write(sb.toString());
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (href.contains(".css") || href.contains(".xml") || href.contains(".ashx") || href.contains(".js") || href.contains("feed")) {
			return false;
		}

		return !FILTERS.matcher(href).matches() && (href.startsWith("http://abcnews.go.com/")
				|| href.startsWith("https://abcnews.go.com/") || href.startsWith("https://www.abcnews.go.com/")
				|| href.startsWith("http://www.abcnews.go.com/"));
	}

	/**
	 * This function is called when a page is fetched and ready to be processed
	 * by your program.
	 */
	@Override
	public void visit(Page page) {
		String url = page.getWebURL().getURL();
		System.out.println("URL: " + url);
		
		try {

			if (!contentTypes.containsKey(page.getContentType().split(";")[0])) {
				contentTypes.put(page.getContentType().split(";")[0], 1);
			} else {
				contentTypes.put(page.getContentType().split(";")[0],
						contentTypes.get(page.getContentType().split(";")[0]) + 1);
			}

			if (page.getContentData().length < 1024) {
				if (!contentSizes.containsKey("<1KB"))
					contentSizes.put("<1KB", 1);
				else
					contentSizes.put("<1KB", contentSizes.get("<1KB") + 1);
			} else if (page.getContentData().length >= 1024 && page.getContentData().length < 10240) {
				if (!contentSizes.containsKey("1KB ~ <10KB"))
					contentSizes.put("1KB ~ <10KB", 1);
				else
					contentSizes.put("1KB ~ <10KB", contentSizes.get("1KB ~ <10KB") + 1);
			} else if (page.getContentData().length >= 10240 && page.getContentData().length < 102400) {
				if (!contentSizes.containsKey("10KB ~ <100KB"))
					contentSizes.put("10KB ~ <100KB", 1);
				else
					contentSizes.put("10KB ~ <100KB", contentSizes.get("10KB ~ <100KB") + 1);
			} else if (page.getContentData().length >= 102400 && page.getContentData().length < 1048576) {
				if (!contentSizes.containsKey("100KB ~ <1MB"))
					contentSizes.put("100KB ~ <1MB", 1);
				else
					contentSizes.put("100KB ~ <1MB", contentSizes.get("100KB ~ <1MB") + 1);
			} else {
				if (!contentSizes.containsKey(">1MB"))
					contentSizes.put(">1MB", 1);
				else
					contentSizes.put(">1MB", contentSizes.get(">1MB") + 1);
			}

			if (page.getParseData() instanceof HtmlParseData || page.getParseData() instanceof BinaryParseData) {
				Set<WebURL> links;
				if(page.getParseData() instanceof HtmlParseData){
					HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
					links = htmlParseData.getOutgoingUrls();
				} else {
					BinaryParseData binaryParseData = (BinaryParseData) page.getParseData();
					links = binaryParseData.getOutgoingUrls();
				}
				
				PrintWriter pw1;

				pw1 = new PrintWriter(new FileWriter("visit.csv", true));

				StringBuilder sb1 = new StringBuilder();
				sb1.append(url);
				sb1.append(',');
				sb1.append(page.getContentData().length);
				sb1.append(',');
				sb1.append(links.size());
				sb1.append(',');
				sb1.append(page.getContentType().split(";")[0]);
				sb1.append('\n');

				pw1.write(sb1.toString());
				pw1.close();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void handlePageStatusCode(WebURL webUrl, int statusCode, String statusDescription) {

		totalFetchesAttempted++;
		PrintWriter pw;
		try {
			pw = new PrintWriter(new FileWriter("fetch.csv", true));

			StringBuilder sb = new StringBuilder();
			sb.append(webUrl.getURL());
			sb.append(',');
			sb.append(statusCode);
			sb.append('\n');
			pw.write(sb.toString());
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (statusCode != 200) {
			fetchesFailed++;
		}
		
		if(statusCode == 200){
			fetchesSucceeded++;
		}

		if (!statusCodes.containsKey(statusCode))
			statusCodes.put(statusCode, 1);
		else
			statusCodes.put(statusCode, statusCodes.get(statusCode) + 1);

		// -----------------------------------------------------------------------------------

	}

	@Override
	public void onBeforeExit() {
		// TODO Auto-generated method stub
		super.onBeforeExit();

		System.out.println("Fetches attempted " + totalFetchesAttempted);
		System.out.println("Fetches succeeded " + fetchesSucceeded);
		System.out.println("Fetches failed " + fetchesFailed);
		System.out.println("Fetches aborted " + fetchesAborted);

		System.out.println("\n Total URLs extracted " + totalUrlsExtracted);
		System.out.println("Unique URLs extracted " + totalUniqueUrls.size());
		System.out.println("Unique URLs extracted within website " + uniqueWithin.size());
		System.out.println("Unique URLs extracted outside website " + uniqueOutside.size());

		System.out.println("\n Different Status codes: ");
		statusCodes.forEach((key, value) -> System.out.println(key + " : " + value));

		System.out.println("\n Different content types: ");
		contentTypes.forEach((key, value) -> System.out.println(key + " : " + value));

		System.out.println("\n Different content sizes: ");
		contentSizes.forEach((key, value) -> System.out.println(key + " : " + value));

	}
}