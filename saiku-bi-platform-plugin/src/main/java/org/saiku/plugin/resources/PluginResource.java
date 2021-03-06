/*
 * Copyright (C) 2011 OSBI Ltd
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 2 of the License, or (at your option) 
 * any later version.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 *
 */
package org.saiku.plugin.resources;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.saiku.datasources.connection.ISaikuConnection;
import org.saiku.datasources.datasource.SaikuDatasource;
import org.saiku.olap.dto.SaikuQuery;
import org.saiku.service.datasource.DatasourceService;
import org.saiku.service.olap.OlapQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pt.webdetails.cda.CdaEngine;
import pt.webdetails.cda.query.QueryOptions;
import pt.webdetails.cda.settings.CdaSettings;

/**
 * QueryServlet contains all the methods required when manipulating an OLAP Query.
 * @author Paul Stoellberger
 *
 */
@Component
@Path("/saiku/{username}/plugin")
@XmlAccessorType(XmlAccessType.NONE)
public class PluginResource {

	private static final Logger log = LoggerFactory.getLogger(PluginResource.class);

	@Autowired
	private OlapQueryService queryService;

	@Autowired
	private DatasourceService datasourceService;

	@GET
	@Produces({"text/plain" })
	@Path("/cda")
	public String getCda(@QueryParam("query") String query) 
	{
		try {
			SaikuQuery sq = queryService.getQuery(query);
			SaikuDatasource ds = datasourceService.getDatasource(sq.getCube().getConnectionName());
			Properties props = ds.getProperties();

			String cdaFile = getCdaAsString(
					props.getProperty(ISaikuConnection.DRIVER_KEY), 
					props.getProperty(ISaikuConnection.URL_KEY),
					sq.getName(),
					sq.getMdx());

			return cdaFile;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	@GET
	@Produces({"application/json" })
	@Path("/cda/execute")
	public Response execute(
			@QueryParam("query") String query,
			@QueryParam("type") String type) 
	{
		try {
			String cdaString = getCda(query);
			Document cda = DocumentHelper.parseText(cdaString);
			
		    final CdaSettings cdaSettings = new CdaSettings(cda, "cda1", null);
		    
		    log.debug("Doing query on Cda - Initializing CdaEngine");
		    final CdaEngine engine = CdaEngine.getInstance();
		    final QueryOptions queryOptions = new QueryOptions();
		    queryOptions.setDataAccessId("1");
		    queryOptions.setOutputType("json");
		    log.info("Doing query");
		    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		    PrintStream printstream = new PrintStream(outputStream);
		    engine.doQuery(printstream, cdaSettings, queryOptions);
			byte[] doc = outputStream.toByteArray();
			
			return Response.ok(doc, MediaType.APPLICATION_JSON).header(
							"content-length",doc.length).build();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Response.serverError().build();


	}


	//	private CdaSettings initCda(String sessionId, String domain) throws Exception {
	//		CdaSettings cda = new CdaSettings("cda" + sessionId, null);
	//
	//		String[] domainInfo = domain.split("/");
	//			Connection connection = new MetadataConnection("1", domainInfo[0] + "/" + domainInfo[1], domainInfo[1]);
	//			Connection con = new jdbcconn
	//			
	//		MqlDataAccess dataAccess = new MqlDataAccess(sessionId, sessionId, "1", "");
	//		//dataAccess.setCacheEnabled(true);
	//		cda.addConnection(connection);
	//		cda.addDataAccess(dataAccess);
	//		return cda;
	//	}


//	private CdaSettings getCdaSettings(String sessionId, SaikuDatasource ds, SaikuQuery query) {
//
//		try {		
//			Document document = DocumentHelper.parseText("");
//
//			return new CdaSettings(document, sessionId, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return null;
//	}

	private Document getCdaAsDocument(String driver, String url, String name, String query) throws Exception {
		String cda = getCdaAsString(driver, url, name, query);
		return DocumentHelper.parseText(cda);
	}

	private String getCdaAsString(String driver, String url, String name, String query) throws Exception {
		String cda = getCdaTemplate();
		cda = cda.replaceAll("@@DRIVER@@", driver);
		cda = cda.replaceAll("@@NAME@@", name);
		cda = cda.replaceAll("@@URL@@", url);
		cda = cda.replaceAll("@@QUERY@@", query);
		return cda;
	}

	private String getCdaTemplate() {
		String cda = 
			"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
			"<CDADescriptor>\n" +
			"   <DataSources>\n" +
			"        <Connection id=\"1\" type=\"olap4j.jdbc\">\n" +
			"            <Driver>@@DRIVER@@</Driver>\n" +
			"            <Url>@@URL@@</Url>\n" +
			"        </Connection>\n" +
			"    </DataSources>\n" +
			"  <DataAccess id=\"1\" connection=\"1\" type=\"olap4j\" access=\"public\">\n" +
			"		<Name>@@NAME@@</Name>\n" +
			"        <Query><![CDATA[" +
			"			@@QUERY@@" +
			"		]]></Query>\n" +
			"    </DataAccess>\n" +
			"</CDADescriptor>\n";

		return cda;
	}
}
