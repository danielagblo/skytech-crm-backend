-- Invoice line item sub-lines (cost breakdowns). Sub-lines are plain text
-- labels, carry no quantity/price, and never contribute to invoice totals.
create table invoice_item_sublines (
  invoice_item_id uuid not null references invoice_items (id) on delete cascade,
  position int not null,
  label varchar(500) not null,
  primary key (invoice_item_id, position)
);