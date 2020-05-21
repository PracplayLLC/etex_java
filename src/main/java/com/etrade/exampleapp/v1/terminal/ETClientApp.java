package com.etrade.exampleapp.v1.terminal;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.etrade.exampleapp.config.OOauthConfig;
import com.etrade.exampleapp.config.SandBoxConfig;
import com.etrade.exampleapp.v1.clients.accounts.AccountListClient;
import com.etrade.exampleapp.v1.clients.accounts.BalanceClient;
import com.etrade.exampleapp.v1.clients.accounts.PortfolioClient;
import com.etrade.exampleapp.v1.clients.market.QuotesClient;
import com.etrade.exampleapp.v1.clients.order.OrderClient;
import com.etrade.exampleapp.v1.clients.order.OrderPreview;
import com.etrade.exampleapp.v1.clients.order.OrderTerm;
import com.etrade.exampleapp.v1.clients.order.PriceType;
import com.etrade.exampleapp.v1.exception.ApiException;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.FileSystems;

public class ETClientApp  {
	
	protected Logger log = Logger.getLogger(ETClientApp.class);
	
	AnnotationConfigApplicationContext ctx = null;
	
	Map<String, String> acctListMap = new HashMap();

	boolean isLive = false;
	
	public final static String lineSeperator = System.lineSeparator();

	public final static PrintStream out = System.out;


	public ETClientApp(String[] args) {
	//	super(args);
		
	}

	public void init(boolean flag){
		try {
	//		addClassPath(".");
			Path path = FileSystems.getDefault().getPath(".").toAbsolutePath().normalize();
			out.println("Current Thread :"+ Thread.currentThread().getName() + ", Id : "+Thread.currentThread().getId()+ " curdir: "+path );

			if( ctx != null)
				ctx.close();

			ctx = new AnnotationConfigApplicationContext();
			// via: https://stackoverflow.com/questions/8934203/how-can-i-make-spring-load-classes-from-a-given-url-for-bean-instantiation
			// and: https://stackoverflow.com/questions/34136699/ikvm-getresourceasstreams-returns-null/34379133#34379133
			//

			if ( flag ) {
				//ctx = new AnnotationConfigApplicationContext();
	//			ctx.setClassLoader(OOauthConfig.class.getClassLoader());
				ctx.register(OOauthConfig.class);
				out.println("regsiter/refresh: "+flag);
				ctx.refresh();
			
			}else {
				//ctx = new AnnotationConfigApplicationContext();
	//			ctx.setClassLoader(SandBoxConfig.class.getClassLoader());
				ctx.register(SandBoxConfig.class);
				out.println("regsiter/refresh: "+flag);
				ctx.refresh();
			}
			
		} catch (Exception e) {
			out.println( " Sorry we are not able to initiate oauth request at this time..");
			StringWriter sw = new StringWriter();
e.printStackTrace(new PrintWriter(sw));
String exceptionAsString = sw.toString();
			out.println("Oauth Initialization failed, err: "+exceptionAsString);
		}
		log.debug(" Context initialized for "+(isLive ? "Live Environment":" Sandbox Environment"));
	}
/*
	public void addClassPath(String cp) {
    String                              seps=File.pathSeparator;                // separators

    if(!File.pathSeparator.equals(";")) { seps+=";"; }                          // want to accept both system separator and ';'
    for(StringTokenizer st=new StringTokenizer(cp,seps,false); st.hasMoreTokens(); ) {
        String pe=st.nextToken();
        File   fe;
        String bn=null;

        if(pe.length()==0) { continue; }

        fe=new File(pe);
        if(fe.getName().indexOf('*')!=-1) {
            bn=fe.getName();
            fe=fe.getParentFile();
            }

        if(!fe.isAbsolute() && pe.charAt(0)!='/' && pe.charAt(0)!='\\') { fe=new File(rootPath,fe.getPath()); }
        try { fe=fe.getCanonicalFile(); }
        catch(IOException thr) {
            log.diagln("Skipping non-existent classpath element '"+fe+"' ("+thr+").");
            continue;
            }
        if(!GenUtil.isBlank(bn)) {
            fe=new File(fe,bn);
            }
        if(classPathElements.contains(fe.getPath())) {
            log.diagln("Skipping duplicate classpath element '"+fe+"'.");
            continue;
            }
        else {
            classPathElements.add(fe.getPath());
            }

        if(!GenUtil.isBlank(bn)) {
            addJars(fe.getParentFile(),bn);
            }
        else if(!fe.exists()) {                                                 // s/never be due getCanonicalFile() above
            log.diagln("Could not find classpath element '"+fe+"'");
            }
        else if(fe.isDirectory()) {
            addURL(createUrl(fe));
            }
        else if(fe.getName().toLowerCase().endsWith(".zip") || fe.getName().toLowerCase().endsWith(".jar")) {
            addURL(createUrl(fe));
            }
        else {
            log.diagln("ClassPath element '"+fe+"' is not an existing directory and is not a file ending with '.zip' or '.jar'");
            }
        }
    log.diagln("Class loader is using classpath: \""+classPath+"\".");
    }

    public void addJars(File dir, String nam) {
    String[]                            jars;                                   // matching jar files

    if(nam.endsWith(".jar")) { nam=nam.substring(0,(nam.length()-4)); }

    if(!dir.exists()) {
        log.diagln("Could not find directory for Class Path element '"+dir+File.separator+nam+".jar'");
        return;
        }
    if(!dir.canRead()) {
        log.error("Could not read directory for Class Path element '"+dir+File.separator+nam+".jar'");
        return;
        }

    FileSelector fs=new FileSelector(true).add("BaseName","EG",nam,true).add("Name","EW",".jar",true);
    if((jars=dir.list(fs))==null) {
        log.error("Error accessing directory for Class Path element '"+dir+File.separator+nam+".jar'");
        }
    else if(jars.length==0) {
        log.diagln("No JAR files match specification '"+new File(dir,nam)+".jar'");
        }
    else {
        log.diagln("Adding files matching specification '"+dir+File.separator+nam+".jar'");
        Arrays.sort(jars,String.CASE_INSENSITIVE_ORDER);
        for(int xa=0; xa<jars.length; xa++) { addURL(createUrl(new File(dir,jars[xa]))); }
        }
    }

private URL createUrl(File fe) {
    try {
        URL url=fe.toURI().toURL();
        log.diagln("Added URL: '"+url.toString()+"'");
        if(classPath.length()>0) { classPath+=File.pathSeparator; }
        this.classPath+=fe.getPath();
        return url;
        }
    catch(MalformedURLException thr) {
        log.diagln("Classpath element '"+fe+"' could not be used to create a valid file system URL");
        return null;
        }
    }*/

	
/*	private void initOOauth() {
		log.debug("Initializing the oauth " );
		try {
			if( sessionData == null) {
				log.debug(" Re-Initalizing oauth ...");
				ctx.refresh();

				OauthController controller = ctx.getBean(OauthController.class);

				controller.fetchToken();

				controller.authorize();
				sessionData = controller.fetchAccessToken();
				log.debug(" Oauth initialized ");
			}
		}catch(Exception e) {
			log.debug(e);
			out.println( " Sorry we are not able to continue at this time, please restart the client..");
		}
	}*/

	public static void main(String[] args) {

		ETClientApp obj = new ETClientApp(args);


		/*if (obj.hasOption("help")) {
			obj.printHelp();
			return;
		}

		try {
			obj.keyMenu(obj);
		} catch (NumberFormatException e) {
			System.out.println(" Main menu : NumberFormatException ");
		} catch (IOException e) {
			System.out.println(" Main menu : System failure ");;
		}*/
		if (obj==null)
			System.out.println("null client, error setting up connection...");
		else
			System.out.println("client ok.");

	}

    public void keyMenu(ETClientApp obj) throws NumberFormatException, IOException {
	if (true)
		return	;
        int choice = 0;
        do {
            //printKeyMenu();
            choice = KeyIn.getKeyInInteger();
            switch (choice) {

                case 1:
                	log.debug(" Initializing sandbox application context..");
                	isLive = false;
    				init(isLive);
            		mainMenu(this);
                    break;
                case 2:
                	isLive = true;
    				init(isLive);
                	log.debug(" Initializing Live application context..");
            		mainMenu(this);
                    break;
                case 'x':
                    choice = 'x';
                    out.println("Goodbye");
                    System.exit(0);
                    break;
                default:
                    out.println("Invalid Option :");
                    choice = 'x';
                    out.println("Goodbye");
                    System.exit(0);
                    break;
            }

        } while (!"x".equals(choice) || choice != 99);

        obj.mainMenu(obj);
    };

	public void mainMenu(ETClientApp obj) throws NumberFormatException, IOException {

		int choice = 0;
		if (true) return;
		String symbol = "";
		do {

		//	printMainMenu();

			choice = KeyIn.getKeyInInteger();


			switch (choice) {

			case 1:
				out.println(" Input selected for main menu : "+ choice);

				getAccountList();
				obj.subMenu(obj);
				choice = 99;
				//choice = 0;
				break;
			case 2:
				out.println(" Input selected for main menu : "+ choice);

				out.print("Please enter Stock Symbol: ");
				symbol = KeyIn.getKeyInString();
				getQuotes(symbol);
				break;
			case 3:
                out.println("Back to previous menu");
                keyMenu(this);
				break;
			default:
				out.println("Invalid Option :");
				choice = 'x';
				out.println("Goodbye");
				System.exit(0);
				break;
			}

		} while (!"x".equals(choice) || choice != 99);
	}



	public void subMenu( ETClientApp obj) throws NumberFormatException, IOException {

		int choice = 0;
		String acctKey = "";
		if (true) return;

		do {

		//	printSubMenu();

			choice = KeyIn.getKeyInInteger();

			out.println(" Input selected for submenu : "+ choice);
			switch (choice) {

			case 1:
				out.print("Please select an account index for which you want to get balances: ");
				acctKey = KeyIn.getKeyInString();
				getBalance(acctKey);
				break;
			case 2:
				out.print("Please select an account index for which you want to get Portfolio: ");
				acctKey = KeyIn.getKeyInString();
				getPortfolio(acctKey);
				break;
			case 3:
			//	printMenu(orderMenu);

				int orderChoice = KeyIn.getKeyInInteger();

				switch(orderChoice) {
				case 1:
					out.print("Please select an account index for which you want to get Orders: ");
					acctKey = KeyIn.getKeyInString();
					getOrders(acctKey);
					break;
				case 2:
					previewOrder();
					break;
				case 3:
					out.println("Back to previous menu");
					//subMenu(this);
					break;
				default:
					//printMenu(orderMenu);
					break;	
				}
				break;
			case 4:
				//choice = 'x';
				out.println("Going to main menu");
				//obj.mainMenu(obj);
				break;
			default:
				//printSubMenu();
				break;
			}

		} while (choice != 4);
	}

	public void previewOrder(){
		OrderPreview client = ctx.getBean(OrderPreview.class);
		if (true) return;
		
		//client.setSessionData(sessionData);

		Map<String,String> inputs = client.getOrderDataMap();

		String accountIdKey = "";

		out.print("Please select an account index for which you want to preview Order: ");
		String acctKeyIndx = KeyIn.getKeyInString();
		try {
			accountIdKey = getAccountIdKeyForIndex(acctKeyIndx);

		}catch(ApiException e) {
			return;
		}
		out.print(" Enter Symbol : ");

		String symbol = KeyIn.getKeyInString();
		inputs.put("SYMBOL", symbol);

		/* Shows Order Action Menu */
		//printMenu(orderActionMenu);
		/* Accept OrderAction choice*/
		//int choice = isValidMenuItem("Please select valid index for Order Action", orderActionMenu);
		int choice = 0;
		
		/* Fills data to service*/
		client.fillOrderActionMenu(choice,inputs);

		out.print(" Enter Quantity : ");
		int qty = KeyIn.getKeyInInteger();
		inputs.put("QUANTITY", String.valueOf(qty));

		/* Shows Order PriceType  Menu */
		//printMenu(orderPriceTypeMenu);

		/* Accept PriceType choice */
		//choice = isValidMenuItem("Please select valid index for price type", orderPriceTypeMenu);

		/* Fills data to service*/
		client.fillOrderPriceMenu(choice,inputs);

		if( choice == 2) {
			out.print(" Enter limit price : ");
			double limitPirce = KeyIn.getKeyInDouble();
			inputs.put("LIMIT_PRICE", String.valueOf(limitPirce));
		}

		/* Shows Order Duration  Menu */
		//printMenu(durationTypeMenu);

		//choice = isValidMenuItem("Please select valid index for Duration type", durationTypeMenu);


		client.fillDurationMenu(choice,inputs);


		client.previewOrder(accountIdKey, inputs);

	}

	

	public void getAccountList() {
		out.println("start getbean getaccountlist");	
		AccountListClient client = ctx.getBean(AccountListClient.class);
		String response = "";
		try {
			response = client.getAccountList();
			out.println(String.format("\n%20s %25s %25s %25s %25s\n", "Number", "AccountId", "AccountIdKey", "AccountDesc", "InstitutionType"));

			try {

				JSONParser jsonParser = new JSONParser();
				JSONObject jsonObject = (JSONObject) jsonParser.parse(response);
				JSONObject acctLstResponse = (JSONObject) jsonObject.get("AccountListResponse");
				JSONObject accounts = (JSONObject) acctLstResponse.get("Accounts");
				JSONArray acctsArr = (JSONArray) accounts.get("Account");
				Iterator itr = acctsArr.iterator();
				long count = 1;
				while (itr.hasNext()) {
					JSONObject innerObj = (JSONObject) itr.next();
					String acctIdKey = (String) innerObj.get("accountIdKey");
					String acctStatus = (String) innerObj.get("accountStatus");
					if (acctStatus != null && !acctStatus.equals("CLOSED")) {
						acctListMap.put(String.valueOf(count), acctIdKey);
						out.println(String.format("%20s %25s %25s %25s %25s\n", count, innerObj.get("accountId"), acctIdKey, innerObj.get("accountDesc"), innerObj.get("institutionType")));
						count++;
					}
				}

			} catch (Exception e) {
				log.error(" Exception on get accountlist : "+e.getMessage());
				log.error(e);
				e.printStackTrace();
			}

		}catch(ApiException e) {
			out.println();
			out.println(String.format("HttpStatus: %20s", e.getHttpStatus()));
			out.println(String.format("Message: %23s", e.getMessage()));
			out.println(String.format("Error Code: %20s", e.getCode()));
			out.println();out.println();
		}catch (Exception e) {
			log.error(" getAccountList : GenericException " ,e);
		}
	}

	public void getBalance(String acctIndex) {

		BalanceClient client = ctx.getBean(BalanceClient.class);
		String response = "";
		String accountIdKey = "";
		try {
			accountIdKey = getAccountIdKeyForIndex(acctIndex);
		}catch(ApiException e) {
			return;
		}

		try {
			log.debug(" Response String : " + response);
			response = client.getBalance(accountIdKey);
			log.debug(" Response String : " + response);

			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(response);
			log.debug(" JSONObject : " + jsonObject);
			//jsonObject.g
			JSONObject balanceResponse = (JSONObject) jsonObject.get("BalanceResponse");
			String accountId = (String) balanceResponse.get("accountId");
			out.println(String.format("%s\t\t\tBalances for %s %s%s", lineSeperator, accountId, lineSeperator, lineSeperator));

			JSONObject computedRec = (JSONObject) balanceResponse.get("Computed");
			JSONObject realTimeVal = (JSONObject) computedRec.get("RealTimeValues");
			if (computedRec.get("accountBalance") != null) {
				if( Double.class.isAssignableFrom(computedRec.get("accountBalance").getClass())) {
					Double accountBalance = (Double)computedRec.get("accountBalance");
					out.println("\t\tCash purchasing power:   $" + accountBalance);
				}else if( Long.class.isAssignableFrom(computedRec.get("accountBalance").getClass())){
					Long accountBalance = (Long)computedRec.get("accountBalance");
					out.println("\t\tCash purchasing power:   $" + accountBalance);
				}
			}
			if (realTimeVal.get("totalAccountValue") != null) {
				if( Double.class.isAssignableFrom(realTimeVal.get("totalAccountValue").getClass())) {
					Double totalAccountValue = (Double)realTimeVal.get("totalAccountValue");
					out.println("\t\tLive Account Value:      $" + totalAccountValue);
				}else if( Long.class.isAssignableFrom(realTimeVal.get("totalAccountValue").getClass())){
					Long totalAccountValue = (Long)realTimeVal.get("totalAccountValue");
					out.println("\t\tLive Account Value:      $" + totalAccountValue);
				}
			}
			if (computedRec.get("marginBuyingPower") != null) {
				if( Double.class.isAssignableFrom(computedRec.get("marginBuyingPower").getClass())) {
					Double marginBuyingPower = (Double)computedRec.get("marginBuyingPower");
					out.println("\t\tMargin Buying Power:     $" + marginBuyingPower);
				}else if( Long.class.isAssignableFrom(computedRec.get("marginBuyingPower").getClass())){
					Long totalAccountValue = (Long)computedRec.get("marginBuyingPower");
					out.println("\t\tMargin Buying Power:     $" + totalAccountValue);
				}
			}
			if (computedRec.get("cashBuyingPower") != null) {
				if( Double.class.isAssignableFrom(computedRec.get("cashBuyingPower").getClass())) {
					Double cashBuyingPower = (Double)computedRec.get("cashBuyingPower");
					out.println("\t\tCash Buying Power:       $" + cashBuyingPower);
				}else if( Long.class.isAssignableFrom(computedRec.get("cashBuyingPower").getClass())){
					Long cashBuyingPower = (Long)computedRec.get("cashBuyingPower");
					out.println("\t\tCash Buying Power:       $" + cashBuyingPower);
				}
			}
			System.out.println("\n");


		}catch(ApiException e) {
			out.println();
			out.println(String.format("HttpStatus: %20s", e.getHttpStatus()));
			out.println(String.format("Message: %23s", e.getMessage()));
			out.println(String.format("Error Code: %20s", e.getCode()));
			out.println();
			out.println();
		}catch (Exception e) {
			log.error(" getBalance : GenericException " ,e);
		}
	}

	public void getPortfolio(String acctIndex) {

		PortfolioClient client = ctx.getBean(PortfolioClient.class);
		String response = "";
		String accountIdKey = "";
		try {
			accountIdKey = getAccountIdKeyForIndex(acctIndex);
		}catch(ApiException e) {
			return;
		}
		try {
			response = client.getPortfolio(accountIdKey);
			log.debug(" Response String : " + response);
			JSONParser jsonParser = new JSONParser();


			JSONObject jsonObject = (JSONObject) jsonParser.parse(response);
			//out.println(" JSONObject : " + jsonObject);
			out.println("*************************************");
			JSONObject portfolioResponse = (JSONObject) jsonObject.get("PortfolioResponse");
			//out.println(" portfolioResponse : " + portfolioResponse);
			out.println("*************************************");
			JSONArray accountPortfolioArr = (JSONArray) portfolioResponse.get("AccountPortfolio");
			Object[] responseData = new Object[8];
			Iterator acctItr = accountPortfolioArr.iterator();

			StringBuilder sbuf = new StringBuilder();
			Formatter fmt = new Formatter(sbuf);

			StringBuilder acctIdBuf = new StringBuilder();

			while(acctItr.hasNext()) {
				JSONObject acctObj = (JSONObject) acctItr.next();

				String accountId = (String) acctObj.get("accountId");

				acctIdBuf.append(lineSeperator).append("\t\t Portfolios for ").append(accountId).append(lineSeperator).append(lineSeperator);

				JSONArray positionArr = (JSONArray) acctObj.get("Position");

				Iterator itr = positionArr.iterator();


				while (itr.hasNext()) {
					StringBuilder formatString = new StringBuilder("");
					JSONObject innerObj = (JSONObject) itr.next();

					JSONObject prdObj = (JSONObject) innerObj.get("Product");
					responseData[0] = prdObj.get("symbol");
					formatString.append("%25s");

					responseData[1] = innerObj.get("quantity");
					formatString.append(" %25s");

					responseData[2] = prdObj.get("securityType");
					formatString.append(" %25s");

					JSONObject quickObj = (JSONObject) innerObj.get("Quick");

					if(Double.class.isAssignableFrom(quickObj.get("lastTrade").getClass())) {
						responseData[3] =  quickObj.get("lastTrade");
						formatString.append(" %25f");
					}else {
						responseData[3] =  quickObj.get("lastTrade");
						formatString.append(" %25d");
					}

					if(Double.class.isAssignableFrom(innerObj.get("pricePaid").getClass())) {
						responseData[4] = innerObj.get("pricePaid");
						formatString.append(" %25f");
					}else {
						responseData[4] = innerObj.get("pricePaid");
						formatString.append(" %25d");
					}
					if(Double.class.isAssignableFrom(innerObj.get("totalGain").getClass())) {
						responseData[5] = innerObj.get("totalGain");
						formatString.append(" %25f");
					}else {
						responseData[5] = innerObj.get("totalGain");
						formatString.append(" %25d");
					}
					if(Double.class.isAssignableFrom(innerObj.get("marketValue").getClass())) {
						responseData[6] =  innerObj.get("marketValue");
						formatString.append(" %25f").append( lineSeperator );
					}else {
						responseData[6] =  innerObj.get("marketValue");
						formatString.append(" %25d").append( lineSeperator );;
					}

					fmt.format(formatString.toString(), responseData[0], responseData[1],responseData[2],responseData[3],responseData[4],responseData[5],responseData[6]);
				}

			}
			out.println(acctIdBuf.toString());

			String titleFormat = new StringBuilder("%25s %25s %25s %25s %25s %25s %25s").append(System.lineSeparator()).toString();

			out.printf(titleFormat, "Symbol","Quantity", "Type", "LastPrice", "PricePaid", "TotalGain","Value");
			out.println(sbuf.toString());
			out.println();
			out.println();

		}catch(ApiException e) {
			out.println();
			out.println(String.format("HttpStatus: %20s", e.getHttpStatus()));
			out.println(String.format("Message: %23s", e.getMessage()));
			out.println(String.format("Error Code: %20s", e.getCode()));
			out.println();out.println();
		}catch (Exception e) {
			log.error(" getPortfolio " ,e);
			out.println();

			if( e instanceof ApiException ) {
				ApiException apie = (ApiException)e;
				out.println();
				out.println(String.format("HttpStatus: %20s", apie.getHttpStatus()));
				out.println(String.format("Message: %23s", apie.getMessage()));
				out.println(String.format("Error Code: %20s", apie.getCode()));
				out.println();out.println();
			}else {
				out.println(String.format("Message: %23s", e.getMessage()));
			}
			out.println();out.println();

		}


	}

	public void getQuotes(String symbol) {
		DecimalFormat format = new DecimalFormat("#.00");

		QuotesClient client = ctx.getBean(QuotesClient.class);
		String response = "";
		try {
			response = client.getQuotes(symbol);
			log.debug(" Response String : " + response);
			try {

				JSONParser jsonParser = new JSONParser();
				JSONObject jsonObject = (JSONObject) jsonParser.parse(response);
				log.debug(" JSONObject : " + jsonObject);

				JSONObject quoteResponse = (JSONObject) jsonObject.get("QuoteResponse");
				JSONArray quoteData = (JSONArray) quoteResponse.get("QuoteData");

				if(quoteData != null) {
                    Iterator itr = quoteData.iterator();
                    while (itr.hasNext()) {
                        out.println();
                        JSONObject innerObj = (JSONObject) itr.next();
                        if (innerObj != null && innerObj.get("dateTime") != null) {
                            String dateTime = (String) (innerObj.get("dateTime"));
                            out.println("Date Time: " + dateTime);
                        }

                        JSONObject product = (JSONObject) innerObj.get("Product");
                        if (product != null && product.get("symbol") != null) {
                            String symbolValue = (String) (product.get("symbol")).toString();
                            out.println("Symbol: " + symbolValue);
                        }

                        if (product != null && product.get("securityType") != null) {
                            String securityType = (String) (product.get("securityType")).toString();
                            out.println("Security Type: " + securityType);
                        }

                        JSONObject all = (JSONObject) innerObj.get("All");
                        if (all != null && all.get("lastTrade") != null) {
                            String lastTrade = (String) (all.get("lastTrade")).toString();
                            out.println("Last Price: " + lastTrade);
                        }

                        if (all != null && all.get("changeClose") != null && all.get("changeClosePercentage") != null) {
                            String changeClose = format.format(all.get("changeClose"));
                            String changeClosePercentage = (String) (all.get("changeClosePercentage")).toString();
                            out.println("Today's Change: " + changeClose + " (" + changeClosePercentage + "%)");
                        }

                        if (all != null && all.get("open") != null) {
                            String open = (String) (all.get("open")).toString();
                            out.println("Open: " + open);
                        }

                        if (all != null && all.get("previousClose") != null) {
                            String previousClose = format.format(all.get("previousClose"));
                            out.println("Previous Close: " + previousClose);
                        }

                        if (all != null && all.get("bid") != null && all.get("bidSize") != null) {
                            String bid = format.format(all.get("bid"));
                            String bidSize = (String) (all.get("bidSize")).toString();
                            out.println("Bid (Size): " + bid + "x" + bidSize);
                        }

                        if (all != null && all.get("ask") != null && all.get("askSize") != null) {
                            String ask = format.format(all.get("ask"));
                            String askSize = (String) (all.get("askSize")).toString();
                            out.println("Ask (Size): " + ask + "x" + askSize);
                        }

                        if (all != null && all.get("low") != null && all.get("high") != null) {
                            String low = format.format(all.get("low"));
                            String high = (String) (all.get("high")).toString();
                            out.println("Day's Range: " + low + "-" + high);
                        }

                        if (all != null && all.get("totalVolume") != null) {
                            String totalVolume = all.get("totalVolume").toString();
                            out.println("Volume: " + totalVolume);
                        }

                        JSONObject mutualFund = (JSONObject) innerObj.get("MutualFund");
                        if (mutualFund != null && mutualFund.get("netAssetValue") != null) {
                            String netAssetValue = (String) (mutualFund.get("netAssetValue")).toString();
                            out.println("Net Asset Value: " + netAssetValue);
                        }

                        if (mutualFund != null && mutualFund.get("changeClose") != null
                                && mutualFund.get("changeClosePercentage") != null ) {
                            String changeClose = format.format(mutualFund.get("changeClose"));
                            String changeClosePercentage = (String) (mutualFund.get("changeClosePercentage")).toString();
                            out.println("Today's Change: " + changeClose + " (" + changeClosePercentage + "%)");
                        }

                        if (mutualFund != null && mutualFund.get("publicOfferPrice") != null) {
                            String publicOfferPrice = (String) (mutualFund.get("publicOfferPrice")).toString();
                            out.println("Public Offer Price: " + publicOfferPrice);
                        }

                        if (mutualFund != null && mutualFund.get("previousClose") != null) {
                            String previousClose = (String) (mutualFund.get("previousClose")).toString();
                            out.println("Previous Close: " + previousClose);
                        }
                    }
                    out.println();
                } else {
                    log.error(" Error : Invalid stock symbol.");
                    out.println("Error : Invalid Stock Symbol.\n");
                }

			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}catch(ApiException e) {
			out.println();
			out.println(String.format("HttpStatus: %20s", e.getHttpStatus()));
			out.println(String.format("Message: %23s", e.getMessage()));
			out.println(String.format("Error Code: %20s", e.getCode()));
			out.println();out.println();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void getOrders(final String acctIndex) {

		OrderClient client = ctx.getBean(OrderClient.class);
		
		String response = "";
		String accountIdKey = "";
		try {
			accountIdKey = getAccountIdKeyForIndex(acctIndex);
		}catch(ApiException e) {
			return;
		}
		try {
			response = client.getOrders(accountIdKey);
			log.debug(" Get Order response : " + response);
			if( response != null ) {

				StringBuilder acctIdBuf = new StringBuilder();

				acctIdBuf.append(lineSeperator).append("\t\t Orders for selected account index : ").append(acctIndex).append(lineSeperator).append(lineSeperator);

				out.println(acctIdBuf.toString());

				client.parseResponse(response);

			}else {
				out.println("No records...");
			}

		}catch(ApiException e) {
			out.println();
			out.println(String.format("HttpStatus: %20s", e.getHttpStatus()));
			out.println(String.format("Message: %23s", e.getMessage()));
			out.println(String.format("Error Code: %20s", e.getCode()));
			out.println();out.println();
		}catch (Exception e) {
			log.error(" getBalance : GenericException " ,e);
			out.println();

			if( e instanceof ApiException ) {
				ApiException apie = (ApiException)e;
				out.println();
				out.println(String.format("HttpStatus: %20s", apie.getHttpStatus()));
				out.println(String.format("Message: %23s", apie.getMessage()));
				out.println(String.format("Error Code: %20s", apie.getCode()));
				out.println();out.println();
			}else {
				out.println(String.format("Message: %23s", e.getMessage()));
			}
			out.println();out.println();

		}
	}



	private String getAccountIdKeyForIndex(String acctIndex) throws ApiException{
		String accountIdKey = "";
		try
		{
			accountIdKey = acctListMap.get(acctIndex);
			if (accountIdKey == null) {
				out.println(" Error : !!! Invalid account index selected !!! ");
			}
		}
		catch(Exception e)
		{
			log.error(" getAccountIdKeyForIndex " ,e);
		}
		if( accountIdKey == null ) {
			throw new ApiException(0,"0","Invalid selection for accountId index");
		}
		return accountIdKey;
	}

	private String getPrice(PriceType priceType, JSONObject orderDetail) {

		String value = "";

		if( PriceType.LIMIT == priceType ) {
			value = String.valueOf(orderDetail.get("limitPrice"));
		}else if( PriceType.MARKET == priceType) {
			value = "Mkt";
		}else {
			value = priceType.getValue();
		}

		return value;

	}
	private String getTerm(OrderTerm orderTerm) {

		String value = "";

		if( OrderTerm.GOOD_FOR_DAY == orderTerm ) {
			value = "Day";
		}else {
			value = orderTerm.getValue();
		}

		return value;

	}
	private String convertLongToDate(Long ldate) {
		LocalDateTime dte = LocalDateTime.ofInstant(Instant.ofEpochMilli(ldate), ZoneId.systemDefault());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy");

        return formatter.format(dte);
	}
}
