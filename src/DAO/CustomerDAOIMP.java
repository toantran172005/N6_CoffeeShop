package DAO;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.regex.Pattern;

import Database.DatabaseConnection;
import Models.CartItems;
import Models.Carts;
import Models.Customers;
import Models.ProductType;
import Models.Products;

public class CustomerDAOIMP implements CustomerDAO {
	private Connection con;

//	Khởi tạo mặc định kết nối với database
	public CustomerDAOIMP() {
		try {
			con = DatabaseConnection.getConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

//	Đóng ứng dụng thì ngắt kết nối
	public void closeDatabase() {
		DatabaseConnection.closeConnection(con);
		System.out.println("Closed the connection to database!");
	}

	@Override
	public ArrayList<Products> getListProductFromDb() {
		Statement sta = null;
		ResultSet rs = null;
		ArrayList<Products> listProduct = new ArrayList<>();
		String sql = "SELECT ProductID, ProductName, ProductTypeID, Price, Quantity, Description, Size, ProductIMG FROM Products "
				+ "WHERE ProductID % 2 = 0" + "OR ProductID IN (17,19)";
		try {
			 sta = con.createStatement();
			 rs = sta.executeQuery(sql);
			while (rs.next()) {
				Products pro = new Products();
				pro.setProductID(rs.getInt(1));
				pro.setProductName(rs.getString(2));
				ProductType type = new ProductType(rs.getInt(3));
				pro.setProductTypeID(type);
				pro.setPrice(rs.getBigDecimal(4).doubleValue());
				pro.setQuantity(rs.getInt(5));
				pro.setDescription(rs.getString(6));
				pro.setSize(rs.getString(7));
				pro.setProductImg(rs.getString(8));
				listProduct.add(pro);
			}
			sta.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return listProduct;

	}

	@Override
	public int loginCustomer(String email, String input) {
		try {
//	    	Đăng nhập với Customer
			String sqlCus = "SELECT * FROM Customers WHERE Email = ?";
			PreparedStatement preCus = con.prepareStatement(sqlCus);
			preCus.setString(1, email);
			ResultSet rsCus = preCus.executeQuery();

			if (rsCus.next()) {
				String pwd = rsCus.getString("Password");
				if (pwd.equals(input))
					return rsCus.getInt(1); // Customer đăng nhập thành công, trả về mã Customer để lấy Cart
				else
					return -404; // Sai mật khẩu
			}

//	        Đăng nhập với Employee nếu không có email trong Customers
			String sqlEmp = "SELECT * FROM Employees WHERE Email = ?";
			PreparedStatement preEmp = con.prepareStatement(sqlEmp);
			preEmp.setString(1, email);
			ResultSet rsEmp = preEmp.executeQuery();

			if (rsEmp.next()) {
				int empID = rsEmp.getInt("EmployeeID");
				// So sánh input với ID
				if (String.valueOf(empID).equals(input))
					return 0; // Employee đăng nhập thành công
				else
					return -404; // Sai mã nhân viên
			}

			return -1; // Email không tồn tại trong cả 2 bảng

		} catch (SQLException e) {
			e.printStackTrace();
			return -101; // Lỗi SQL Server
		}
	}

	@Override
	public int registerCustomer(String cusName, String email, String pwd, String phone, String address) {
		// 1: cusName, 2: email, 3: password, 4: phone, 5 address
		String[] regex = { "^[\\p{L} ]{2,}$", "^(?!_|\\.)[\\w.+%-]+@[a-zA-Z0-9.-]{2,}(\\.[a-z]{2,})+$",
				"^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$", "^[0-9]{10}$", "^[\\p{L}0-9 /.-]{2,}$" };
		if (!Pattern.matches(regex[0], cusName))
			return 1; // sai tên
		if (!Pattern.matches(regex[1], email))
			return 2; // sai email
		if (!Pattern.matches(regex[2], pwd))
			return 3; // sai password
		if (!Pattern.matches(regex[3], phone))
			return 4; // sai phone
		if (!Pattern.matches(regex[4], address))
			return 5; // sai địa chỉ

		try {
//			Kiểm tra email tồn tại chưa?
			String checkEmail = "SELECT 1 FROM Customers WHERE Email = ?";
			PreparedStatement preEmail = con.prepareStatement(checkEmail);
			preEmail.setString(1, email);
			ResultSet re = preEmail.executeQuery();
			if (re.next()) {
				preEmail.close();
				re.close();
				return -1; // email tồn tại trả về -1
			}
//			Email chưa tồn tại
			String sql = "INSERT INTO Customers(CustomerName,Phone,Email,Password,Address) VALUES (?,?,?,?,?)";
			PreparedStatement pre = con.prepareStatement(sql);
			pre.setString(1, cusName);
			pre.setString(2, phone);
			pre.setString(3, email);
			pre.setString(4, pwd);
			pre.setString(5, address);
			pre.executeUpdate();
			pre.close();
			return 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return -2; // lỗi trả về -2
		}

	}

	@Override
	public String forgotPassword(String email) {
		String sql = "SELECT Password From Customers WHERE Email = ?";
		try {
			PreparedStatement pre = con.prepareStatement(sql);
			pre.setString(1, email);
			ResultSet rs = pre.executeQuery();

			if (rs.next()) { // Tìm thấy Email
				return rs.getString(1);
			} else
				return null; // Không tìm thấy email
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public boolean isCustomer(int id) {
		try {
//	    	Tìm xem phải Cus không
			String sqlCus = "SELECT * FROM Customers WHERE CustomerID = ?";
			PreparedStatement preCus = con.prepareStatement(sqlCus);
			preCus.setInt(1, id);
			ResultSet rsCus = preCus.executeQuery();

			if (rsCus.next()) {
				rsCus.close();
				return true;
			}
			else {
				rsCus.close();
				return false;
			}

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void addProductToCart(int customerId, Products product, int quantity) {
	    PreparedStatement pre = null;
	    ResultSet rs = null;

	    try {

	        if(customerId == 0) return; // Chưa đăng nhập thì giỏ hàng trống
	        int cartId = 0;
	        String getCartQuery = "SELECT CartID FROM Carts WHERE CustomerID = ?";
	        pre = con.prepareStatement(getCartQuery);
	        pre.setInt(1, customerId);
	        rs = pre.executeQuery();

	        if (rs.next()) {
	            cartId = rs.getInt("CartID");
	        }

//	         2. Kiểm tra nếu sản phẩm đã có trong CartItem, thì cập nhật số lượng
	        String checkItemQuery = "SELECT Quantity FROM CartItems WHERE CartID = ? AND ProductID = ?";
	        pre = con.prepareStatement(checkItemQuery);
	        pre.setInt(1, cartId);
	        pre.setInt(2, product.getProductID());
	        rs = pre.executeQuery();

	        if (rs.next()) {
//	             Đã tồn tại thì cập nhật số lượng
	            int existQuantity = rs.getInt("Quantity");
	            int newQuantity = existQuantity + quantity;

	            String updateQuery = "UPDATE CartItems SET Quantity = ? WHERE CartID = ? AND ProductID = ?";
	            pre = con.prepareStatement(updateQuery);
	            pre.setInt(1, newQuantity);
	            pre.setInt(2, cartId);
	            pre.setInt(3, product.getProductID());
	            pre.executeUpdate();

	        } else {
	            // Chưa có thì insert mới
	            String insertQuery = "INSERT INTO CartItems (CartID, ProductID, Quantity) VALUES (?, ?, ?)";
	            pre = con.prepareStatement(insertQuery);
	            pre.setInt(1, cartId);
	            pre.setInt(2, product.getProductID());
	            pre.setInt(3, quantity);
	            pre.executeUpdate();
	        }
	        
	        pre.close();
	        rs.close();

	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}


	@Override
	public int updateCustomerInfo(Customers customer) {

		String[] regex = { "^[\\p{L} ]{2,}$", "^(?!_|\\.)[\\w.+%-]+@[a-zA-Z0-9.-]{2,}(\\.[a-z]{2,})+$", "^[0-9]{10}$",
				"^[\\p{L}0-9 /.-]{2,}$" };
		if (!Pattern.matches(regex[0], customer.getCustomerName()))
			return 1; // sai tên
		if (!Pattern.matches(regex[1], customer.getEmail()))
			return 2; // sai email
		if (!Pattern.matches(regex[2], customer.getPhone()))
			return 3; // sai phone
		if (!Pattern.matches(regex[3], customer.getAddress()))
			return 4; // sai địa chỉ

		try {
				String update = "UPDATE Customers set CustomerName = ?, Phone = ?, Email = ?, Address = ?"
						+ "WHERE CustomerID = ?";
				PreparedStatement pre = con.prepareStatement(update);
				pre.setString(1, customer.getCustomerName());
				pre.setString(2, customer.getPhone());
				pre.setString(3, customer.getEmail());
				pre.setString(4, customer.getAddress());
				pre.setInt(5, customer.getCustomerID());
				pre.executeUpdate();
				pre.close();
				return 0;
			
		} catch (SQLException e) {
			return -2; // Lỗi trả về -2
		}

		
	}

	@Override
	public boolean deleteCustomer(int customerId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean payOrder(int customerId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Customers getCustomer(int customerID) {
		Customers cus = new Customers();
		try {
			String sql = "SELECT CustomerName, Phone, Email, Address, CreateDate From Customers WHERE CustomerID = ?";
			PreparedStatement pre = con.prepareStatement(sql);
			pre.setInt(1, customerID);
			ResultSet rs = pre.executeQuery();
			if (rs.next()) {
				cus.setCustomerID(customerID);
				cus.setCustomerName(rs.getString(1));
				cus.setPhone(rs.getString(2));
				cus.setEmail(rs.getString(3));
				cus.setAddress(rs.getString(4));
				cus.setPassword("");
				Date creatDate = rs.getDate(5);
				LocalDate date = creatDate.toLocalDate();
				cus.setCreateDate(date);
			}
			pre.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return cus;
	}

	@Override
	public ArrayList<CartItems> getCartItemsByCustomerID(int customerID) {
		ArrayList<CartItems> cartItems = new ArrayList<>();
		try {
			int cartId = 0;
	        String getCartQuery = "SELECT CartID FROM Carts WHERE CustomerID = ?";
	        PreparedStatement preCart = con.prepareStatement(getCartQuery);
	        preCart.setInt(1, customerID);
	        ResultSet rsCart = preCart.executeQuery();

	        if (rsCart.next()) {
	            cartId = rsCart.getInt("CartID");
	            preCart.close();
	            rsCart.close();
	        }
	        
			String sql = "SELECT p.ProductName, p.ProductIMG, p.Price, ci.Quantity "+
					"FROM dbo.CartItems ci JOIN dbo.Products p ON ci.ProductID = p.ProductID "+
					"WHERE ci.CartID = ?";
			PreparedStatement pre = con.prepareStatement(sql);
			pre.setInt(1, cartId);
			ResultSet rs = pre.executeQuery();
			while(rs.next()) {
				String productName = rs.getString(1);
				String img = rs.getString(2);
				double price = rs.getBigDecimal(3).doubleValue();
				int quantity = rs.getInt(4);
				Products p = new Products(0, productName, new ProductType(), price, 0, "", "", img);
				CartItems item = new CartItems(0, new Carts(), p, quantity);
				cartItems.add(item);
			}
			
			pre.close();
			rs.close();
		} catch (SQLException e) {
			// TODO: handle exception
		}
		
		return cartItems;
	}

}
