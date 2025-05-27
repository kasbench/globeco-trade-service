# Supplemental Requirement 4

In this requirement, we will be modifying the POST api/v1/tradeOrder{id}/submit API.

The schema has been modified to add a new column to the trade_order table:

	quantity_sent decimal(18,8) NOT NULL DEFAULT 0

The field name should be quantitySent.

We will implement the following rules:

1. When posting, the value of quantity in the payload may not exceed the value of trade_order.quantity - trade_order.quantity_sent.  If it does, return an appropriate HTTP response and the error message "Requested quantity exceeds available quantity"
2. If the POST is successful, increase trade_order.quantity_sent by the amount of quantity in the payload.
3. Only set trade_order.submitted to true if trade_order.quantity = trade_order.quantity_sent
4. Update the TradeOrderResponseDTO to reflect the new field.


Please update README.md

