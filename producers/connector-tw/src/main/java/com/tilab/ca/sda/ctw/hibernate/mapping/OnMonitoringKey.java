/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tilab.ca.sda.ctw.hibernate.mapping;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="on_monitoring_keys")
public class OnMonitoringKey implements Serializable{

	private static final long serialVersionUID = 9139793578179045248L;

	@Id
	@Column(name="key")
	private String key=null;
	
	@Column(name="on_monitoring_from")
	private Date onMonitoringFrom=null;

	@Column(name="monitor_from_node")
	private String monitorFromNode=null;

	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Date getOnMonitoringFrom() {
		return onMonitoringFrom;
	}

	public void setOnMonitoringFrom(Date onMonitoringFrom) {
		this.onMonitoringFrom = onMonitoringFrom;
	}

	public String getMonitorFromNode() {
		return monitorFromNode;
	}

	public void setMonitorFromNode(String monitorFromNode) {
		this.monitorFromNode = monitorFromNode;
	}
}
