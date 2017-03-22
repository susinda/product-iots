/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.coffeeking.connectedcup.plugin.impl.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.coffeeking.connectedcup.plugin.constants.ConnectedCupConstants;
import org.coffeeking.connectedcup.plugin.exception.ConnectedCupDeviceMgtPluginException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class ConnectedCupDAOUtil {

	private static final Log log = LogFactory.getLog(ConnectedCupDAOUtil.class);
	static DataSource dataSource;
	private static ThreadLocal<Connection> currentConnection = new ThreadLocal<Connection>();

	public ConnectedCupDAOUtil() {
		initConnectedCupDAO();
	}

	public static void initConnectedCupDAO() {
		try {
			Context ctx = new InitialContext();
			dataSource = (DataSource) ctx.lookup(ConnectedCupConstants.DATA_SOURCE_NAME);
		} catch (NamingException e) {
			log.error("Error while looking up the data source: " +
							  ConnectedCupConstants.DATA_SOURCE_NAME);
		}

	}


	public ConnectedCupDAO getConnectedCupDeviceDAO() {
		return new ConnectedCupDAO();
	}

	public static void beginTransaction() throws ConnectedCupDeviceMgtPluginException {
		try {
			Connection conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			currentConnection.set(conn);
		} catch (SQLException e) {
			throw new ConnectedCupDeviceMgtPluginException(
					"Error occurred while retrieving datasource connection", e);
		}
	}

	public static Connection getConnection() throws ConnectedCupDeviceMgtPluginException {
		if (currentConnection.get() == null) {
			try {
				currentConnection.set(dataSource.getConnection());
			} catch (SQLException e) {
				throw new ConnectedCupDeviceMgtPluginException(
						"Error occurred while retrieving data source connection", e);
			}
		}
		return currentConnection.get();
	}

	public static void commitTransaction() throws ConnectedCupDeviceMgtPluginException {
		try {
			Connection conn = currentConnection.get();
			if (conn != null) {
				conn.commit();
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Datasource connection associated with the current thread is null, " +
									"hence commit has not been attempted");
				}
			}
		} catch (SQLException e) {
			throw new ConnectedCupDeviceMgtPluginException(
					"Error occurred while committing the transaction", e);
		} finally {
			closeConnection();
		}
	}

	public static void closeConnection() throws ConnectedCupDeviceMgtPluginException {

		Connection con = currentConnection.get();
		if (con != null) {
			try {
				con.close();
			} catch (SQLException e) {
				log.error("Error occurred while close the connection");
			}
		}
		currentConnection.remove();
	}

	public static void rollbackTransaction() throws ConnectedCupDeviceMgtPluginException {
		try {
			Connection conn = currentConnection.get();
			if (conn != null) {
				conn.rollback();
			} else {
				if (log.isDebugEnabled()) {
					log.debug(
							"Datasource connection associated with the current thread is null, " +
                                    "hence rollback has not been attempted");
				}
			}
		} catch (SQLException e) {
			throw new ConnectedCupDeviceMgtPluginException("Error occurred while rollback the transaction", e);
		} finally {
			closeConnection();
		}
	}
}