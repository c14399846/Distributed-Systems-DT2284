// (C) Oleg Petcov C14399846
// 23 November 2017

/*
* Item object for Bidding items
*
* Basic get and set stuff here
*/

import java.io.*;
import java.math.BigDecimal;

//
public class Item implements Serializable {

	protected String name;
	protected BigDecimal price;

	public Item() {
		this("", new BigDecimal(0.00));
	}
	
	public Item(String name, BigDecimal price) {
		this.name = name;
		this.price = price;
	}
	

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public BigDecimal getPrice() {
		return price;
	}
	
	public void setPrice(BigDecimal price) {
		this.price = price;
	}

}