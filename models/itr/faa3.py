from dataclasses import dataclass
from models.org import Organization
from models.purchase import Purchase


@dataclass
class FAA3:
    org: Organization
    purchase: Purchase
    purchase_price: float
    peak_price: float
    closing_price: float
    sales_proceeds: float = 0.0  # New field for sales/redemption proceeds
