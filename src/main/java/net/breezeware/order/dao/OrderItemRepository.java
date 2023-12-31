package net.breezeware.order.dao;

import net.breezeware.DataBaseConnection;
import net.breezeware.food.entity.FoodItem;
import net.breezeware.order.dto.FoodItemDto;
import net.breezeware.order.dto.OrderCancelDto;
import net.breezeware.order.dto.OrderUpdateDto;
import net.breezeware.order.dto.OrderViewResponseDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * The OrderListRepository class provides data access methods for managing order items and their quantities.
 */
public class OrderItemRepository {
    Connection connection;
    public static final String ORDER_ITEM_TABLE = "order_item";

    /**
     * Gets the cost and available quantity of food items in a list.
     *
     * @param foodItems The list of food items to get cost and quantity information for.
     * @return The list of food items with updated cost and quantity information.
     */
    public List<FoodItemDto> getFoodItemCost(List<FoodItemDto> foodItems) {
        try {
            connection = DataBaseConnection.getConnection();
            assert connection != null;
            for (int i = 0; i < foodItems.size(); i++) {
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM food_item WHERE id=" + foodItems.get(i).getFoodItemId());
                if (resultSet.next()) {
                    foodItems.get(i).setFoodCost(resultSet.getDouble("cost"));
                    foodItems.get(i).setTotalQuantity(resultSet.getInt("quantity"));
                    if (foodItems.get(i).getTotalQuantity() < foodItems.get(i).getFoodItemQuantity()) {
                        System.out.println(resultSet.getString("name") + " is less than Your Required Quantity.");
                        foodItems.get(i).setFoodItemId(-(i));
                    }
                } else {
                    System.out.println("No Food Item Available with the Given Id.");
                    foodItems.get(i).setFoodItemId(-(i));
                }
                resultSet.close();
                statement.close();
            }
            connection.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return foodItems;
    }

    /**
     * Adds food order items to an order.
     *
     * @param orderId   The ID of the order to add items to.
     * @param foodItems The list of food items to add to the order.
     */
    public void addFoodOrderItem(int orderId, List<FoodItemDto> foodItems) {
        try {
            connection = DataBaseConnection.getConnection();
            assert connection != null;
            for (var foodItem : foodItems) {
                if (foodItem.getFoodItemId() > 0) {
                    PreparedStatement preparedStatement = connection.prepareStatement(
                            "INSERT INTO " + ORDER_ITEM_TABLE + " (order_id,food_item_id,quantity,cost) VALUES (?,?,?,?)");
                    preparedStatement.setInt(1, orderId);
                    preparedStatement.setInt(2, foodItem.getFoodItemId());
                    preparedStatement.setInt(3, foodItem.getFoodItemQuantity());
                    preparedStatement.setDouble(4, (foodItem.getFoodCost() * foodItem.getFoodItemQuantity()));
                    preparedStatement.execute();
                    preparedStatement.close();
                }
            }
            connection.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Updates the total cost of an order.
     *
     * @param orderId The ID of the order to update the total cost for.
     * @return The number of records changed in the database.
     */
    public int updateOrderCost(int orderId) {
        int recordChange = 0;
        double totalCost = 0;
        try {
            connection = DataBaseConnection.getConnection();
            assert connection != null;
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + ORDER_ITEM_TABLE + " WHERE order_id=" + orderId);
            while (resultSet.next()) {
                totalCost += resultSet.getDouble("cost");
            }
            resultSet.close();
            statement.close();
            PreparedStatement preparedStatement = connection
                    .prepareStatement("UPDATE \"order\" SET total_cost=? WHERE id=" + orderId);
            preparedStatement.setDouble(1, totalCost);
            recordChange = preparedStatement.executeUpdate();
            preparedStatement.close();
            connection.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return recordChange;
    }

    /**
     * Updates the quantity of food items after an order.
     *
     * @param foodItems The list of food items to update quantities for.
     * @return The number of records changed in the database.
     */
    public int updateFoodItemQuantity(List<FoodItemDto> foodItems) {
        int foodItemChange = 0;
        try {
            connection = DataBaseConnection.getConnection();
            assert connection != null;
            for (var foodItem : foodItems) {
                if (foodItem.getFoodItemId() > 0) {
                    PreparedStatement preparedStatement = connection
                            .prepareStatement("UPDATE food_item SET quantity=? WHERE id=" + foodItem.getFoodItemId());
                    preparedStatement.setInt(1, foodItem.getTotalQuantity() - foodItem.getFoodItemQuantity());
                    int i = preparedStatement.executeUpdate();
                    foodItemChange += i;
                    preparedStatement.close();
                }
            }
            connection.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return foodItemChange;
    }

    /**
     * Views the food items associated with an order.
     *
     * @param orderId The ID of the order to view items for.
     * @return A list of food items associated with the order.
     */
    public List<FoodItem> viewOrderItems(int orderId) {
        List<OrderViewResponseDto> orderViewResponses = new ArrayList<>();
        List<FoodItem> foodItems = new ArrayList<>();
        try {
            connection = DataBaseConnection.getConnection();
            assert connection != null;
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + ORDER_ITEM_TABLE + " WHERE order_id=" + orderId);
            while (resultSet.next()) {
                orderViewResponses.add(new OrderViewResponseDto(resultSet.getInt("food_item_id"),
                        resultSet.getInt("quantity"), resultSet.getDouble("cost")));
            }
            resultSet.close();
            statement.close();
            for (var orderViewResponse : orderViewResponses) {
                Statement statement1 = connection.createStatement();
                ResultSet resultSet1 = statement1.executeQuery("SELECT * FROM food_item WHERE id=" + orderViewResponse.getFoodItemId());
                if (resultSet1.next()) {
                    foodItems.add(new FoodItem(orderViewResponse.getFoodItemId(), resultSet1.getString("name"),
                            orderViewResponse.getFoodItemCost(), orderViewResponse.getFoodItemQuantity()));
                }
                resultSet1.close();
                statement1.close();
            }
            connection.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return foodItems;
    }

    /**
     * Gets the cost and available quantity of a food item.
     *
     * @param orderUpdateDto The order update DTO containing information about the food item.
     * @return The number of records changed in the database.
     */
    public int getFoodItemCost(OrderUpdateDto orderUpdateDto) {
        int quantity = 0;
        boolean isChanged=true;
        int recordChanged = 0;
        try {
            connection = DataBaseConnection.getConnection();
            assert connection != null;
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                    "SELECT * FROM " + ORDER_ITEM_TABLE + " WHERE order_id=" + orderUpdateDto.getOrderId()
                            + " AND food_item_id=" + orderUpdateDto.getFoodItemId());
            if (resultSet.next()) {
                orderUpdateDto.setOldQuantity(resultSet.getInt("quantity"));
            }
            resultSet.close();
            statement.close();
            Statement statement1 = connection.createStatement();
            ResultSet resultSet1 = statement1
                    .executeQuery("SELECT * FROM food_item WHERE id=" + orderUpdateDto.getFoodItemId());
            if (resultSet1.next()) {
                orderUpdateDto.setCost(resultSet1.getDouble("cost"));
                int extraQuantity = orderUpdateDto.getNewQuantity() - orderUpdateDto.getOldQuantity();
                if (orderUpdateDto.getOldQuantity() < orderUpdateDto.getNewQuantity()) {
                    if (orderUpdateDto.getNewQuantity() - orderUpdateDto.getOldQuantity() > resultSet1.getInt("quantity")) {
                        System.out.println(resultSet1.getString("name") + " is less than Your Required Quantity.");
                        quantity=resultSet1.getInt("quantity");
                        isChanged=false;
                    } else {
                        quantity = resultSet1.getInt("quantity") - extraQuantity;
                    }
                } else {
                    quantity = resultSet1.getInt("quantity") - extraQuantity;
                }
            }
            resultSet1.close();
            statement1.close();
            PreparedStatement preparedStatement = connection
                    .prepareStatement("UPDATE food_item SET quantity=? WHERE id=" + orderUpdateDto.getFoodItemId());
            preparedStatement.setInt(1, quantity);
            recordChanged = preparedStatement.executeUpdate();
            preparedStatement.close();
            connection.close();
            if(!isChanged){
                recordChanged=0;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return recordChanged;
    }

    /**
     * Updates the quantity and cost of an order item.
     *
     * @param updateDate The order update DTO containing information about the update.
     * @return The number of records changed in the database.
     */
    public int updateOrderItem(OrderUpdateDto updateDate) {
        int recordsChanged = 0;
        try {
            connection = DataBaseConnection.getConnection();
            assert connection != null;
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE " + ORDER_ITEM_TABLE
                    + " SET quantity=?,cost=? WHERE order_id=" + updateDate.getOrderId() + " AND food_item_id="
                    + updateDate.getFoodItemId());
            preparedStatement.setInt(1, updateDate.getNewQuantity());
            preparedStatement.setDouble(2, (updateDate.getCost() * updateDate.getNewQuantity()));
            recordsChanged = preparedStatement.executeUpdate();
            preparedStatement.close();
            connection.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return recordsChanged;
    }

    /**
     * Retrieves a list of order items to cancel for a given order.
     *
     * @param orderId The ID of the order to retrieve cancelable items for.
     * @return A list of order cancel DTOs.
     */
    public List<OrderCancelDto> cancelFoodItemList(int orderId) {
        List<OrderCancelDto> orderCancelDtos = new ArrayList<>();
        try {
            connection = DataBaseConnection.getConnection();
            assert connection != null;
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + ORDER_ITEM_TABLE + " WHERE order_id=" + orderId);
            while (resultSet.next()) {
                orderCancelDtos.add(new OrderCancelDto(resultSet.getInt("food_item_id"), resultSet.getInt("quantity")));
            }
            connection.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return orderCancelDtos;
    }

    /**
     * Changes the quantity of food items after canceling an order.
     *
     * @param orderCancelDtos The list of order cancel DTOs containing information about the items to cancel.
     * @return The number of records changed in the database.
     */
    public int changeFoodItemQuantity(List<OrderCancelDto> orderCancelDtos) {
        int recordsChanged = 0;
        try {
            connection = DataBaseConnection.getConnection();
            assert connection != null;
            for (var orderCancelDto : orderCancelDtos) {
                int totalQuantity = 0;
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM food_item WHERE id=" + orderCancelDto.getFoodItemId());
                if (resultSet.next()) {
                    totalQuantity = resultSet.getInt("quantity");
                }
                resultSet.close();
                statement.close();
                PreparedStatement preparedStatement = connection
                        .prepareStatement("UPDATE food_item SET quantity=? WHERE id=" + orderCancelDto.getFoodItemId());
                preparedStatement.setInt(1, totalQuantity + orderCancelDto.getQuantity());
                int recordChanged = preparedStatement.executeUpdate();
                recordsChanged += recordChanged;
                preparedStatement.close();
            }
            connection.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return recordsChanged;
    }
}
