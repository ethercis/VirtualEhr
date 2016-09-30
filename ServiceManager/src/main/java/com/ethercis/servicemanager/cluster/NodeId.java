/*
 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
This code is a refactoring and adaptation of the original
work provided by the XmlBlaster project (see http://xmlblaster.org)
for more details.
This code is therefore supplied under LGPL 2.1
 */

/**
 * Project: EtherCIS system application
 * 
 * @author <a href="mailto:christian@adoc.co.th">Christian Chevalley</a>
 * @author <a href="mailto:michele@laghi.eu">Michele Laghi</a>
 * @author <a href="mailto:xmlblast@marcelruff.info">Marcel Ruff</a>
 */


package com.ethercis.servicemanager.cluster;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Holds the unique name of a server instance (= cluster node)
 */
public final class NodeId implements Comparable, java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private static Logger log = LogManager.getLogger(NodeId.class);
	private static final String ME = "NodeId";

	/**
	 * @see #setId(String)
	 */
	public NodeId(String id) {
		setId(id);
	}

	/**
	 * @return e.g. "heron" (/node/heron/... is stripped)
	 */
	public final String getId() {
		return id;
	}

	/**
	 * You need to pass a valid node name without any special characters, e.g.
	 * "http://xy.com:3412" is not allowed logonservice it contains '/' and ':' chars.
	 * 
	 * @param id
	 *            The cluster node id, e.g. "heron".<br />
	 *            If you pass "/node/heron/client/joe" everything is stripped to
	 *            get "heron"
	 * @see org.ClusterController.util.Global#getStrippedId()
	 */
	public final void setId(String id) {
		if (id == null || id.length() < 1) {
			log.error("Cluster node has no name please specify one with -cluster.node.id XXX");
			id = "NoNameNode";
		}
		this.id = id;
		if (this.id.startsWith("/node/"))
			this.id = this.id.substring("/node/".length()); // strip leading
															// "/node/"

		int index = this.id.indexOf("/"); // strip tailing tokens, e.g. from
											// "heron/client/joe" make a "heron"
		if (index == 0) {
			throw new IllegalArgumentException(ME
					+ ": The given cluster node ID '" + id
					+ "' may not start with a '/'");
		}
		if (index > 0) {
			this.id = this.id.substring(0, index);
		}
	}

	/**
	 * @see #getId()
	 */
	public final String toString() {
		return getId();
	}

	/**
	 * Needed for use in TreeSet and TreeMap, enforced by java.lang.Comparable
	 */
	public final int compareTo(Object obj) {
		NodeId n = (NodeId) obj;
		return toString().compareTo(n.toString());
	}

	public final boolean equals(NodeId n) {
		if (id == null) {
			if (n == null || n.getId() == null)
				return true;
			return false;
		}
		if (n == null || n.getId() == null)
			return false;
		return id.equals(n.getId());
	}

	private String id;
}
