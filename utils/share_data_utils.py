from utils.runtime_utils import warn_missing_module

warn_missing_module("pandas")
import pandas as pd
import os
import typing as t

from . import date_utils, logger
from .ticker_mapping import ticker_currency_info
from .rates import rbi_rates_utils


def __validate_dates(
    historic_entry_time_in_ms: int,
    desired_purchase_time_in_ms: int,
    used_fmv_time_in_ms: int,
):
    if historic_entry_time_in_ms > desired_purchase_time_in_ms:
        raise AssertionError(
            f"Historical FMV date {date_utils.log_timestamp(historic_entry_time_in_ms)} "
            + "can NOT be newer than purchase date "
            + f"= {date_utils.log_timestamp(desired_purchase_time_in_ms)}"
        )
    days_diff = (
        date_utils.last_work_day_in_ms(desired_purchase_time_in_ms)
        - historic_entry_time_in_ms
    ) / (24 * 60 * 60 * 1000)

    date_utils.last_work_day_in_ms(desired_purchase_time_in_ms)

    if days_diff > 0:
        msg = (
            f"Historical FMV at {date_utils.log_timestamp(desired_purchase_time_in_ms)} "
            + "was NOT available(maybe due to Public Holiday or weekends) last available data is "
            + f"{int(days_diff)} days old(on {date_utils.display_time(historic_entry_time_in_ms)})"
        )
        logger.log(msg)
        logger.log(
            f"Hence using the next available FMV at {date_utils.log_timestamp(used_fmv_time_in_ms)}"
        )
        # if days_diff > 2:
        #     raise Exception(msg)


TimedFmv = t.TypedDict("TimedFmv", {"entry_time_in_millis": int, "fmv": float})


TimedFmvWithInrRate = t.TypedDict(
    "TimedFmvWithInrRate",
    {
        "entry_time_in_millis": int,
        "fmv": float,
        "inr_rate": float,
    },
)


price_map_cache: t.Dict[str, t.List[TimedFmv]] = {}
usd_inr_cache: t.List[TimedFmv] = []


def __parse_date_from_adobe_format(date_str: str) -> int:
    """
    Parse date from adobe_price_history.csv format: MM/DD/YYYY
    """
    from datetime import datetime
    date_time = datetime.strptime(date_str, "%m/%d/%Y")
    return date_utils.epoch_in_ms(date_time)


def __parse_date_from_usd_inr_format(date_str: str) -> int:
    """
    Parse date from usd_inr_price_history.csv format: DD-MM-YYYY
    """
    from datetime import datetime
    date_time = datetime.strptime(date_str, "%d-%m-%Y")
    return date_utils.epoch_in_ms(date_time)


def __init_usd_inr_map() -> t.List[TimedFmv]:
    """
    Initialize USD to INR exchange rate cache
    """
    global usd_inr_cache
    if not usd_inr_cache:
        print("Parsing USD/INR exchange rate map")
        script_path = os.path.realpath(os.path.dirname(__file__))
        usd_inr_path = os.path.join(
            script_path,
            os.pardir,
            "historic_data",
            "usd_inr_price_history.csv",
        )
        if not os.path.exists(usd_inr_path):
            raise AssertionError(
                f"USD/INR historical data NOT present at {usd_inr_path}"
            )
        df = pd.read_csv(usd_inr_path)

        for _, data in df.iterrows():
            entry_time_in_ms = __parse_date_from_usd_inr_format(data["Date"])
            # Clean the price value (remove any formatting)
            price_str = str(data["Price"]).replace(',', '')
            usd_inr_cache.append(
                {"entry_time_in_millis": entry_time_in_ms, "fmv": float(price_str)}
            )

        # Sort by date for efficient lookup
        usd_inr_cache.sort(key=lambda x: x["entry_time_in_millis"])

    return usd_inr_cache


def __init_map(ticker: str) -> t.List[TimedFmv]:
    if ticker not in price_map_cache:
        print(f"Parsing FMV price map for ticker = {ticker}")
        ticker_price_map: t.List[TimedFmv] = []
        script_path = os.path.realpath(os.path.dirname(__file__))
        
        # Use the new adobe_price_history.csv file
        if ticker.lower() == "adbe":
            historic_share_path = os.path.join(
                script_path,
                os.pardir,
                "historic_data",
                "adobe_price_history.csv",
            )
        else:
            # Fallback to old structure for other tickers
        historic_share_path = os.path.join(
            script_path,
            os.pardir,
            "historic_data",
            "shares",
            ticker.lower(),
            "data.csv",
        )
        
        if not os.path.exists(historic_share_path):
            raise AssertionError(
                f"Historic share data for share {ticker} NOT present at {historic_share_path}"
            )
        df = pd.read_csv(historic_share_path)

        for _, data in df.iterrows():
            if ticker.lower() == "adbe":
                # Handle adobe_price_history.csv format
                entry_time_in_ms = __parse_date_from_adobe_format(data["Date"])
                # Clean the Close/Last value (remove $ sign if present)
                close_price_str = str(data["Close/Last"]).replace('$', '').replace(',', '')
                price = float(close_price_str)
            else:
                # Handle old data.csv format
            entry_time_in_ms = date_utils.parse_yyyy_mm_dd(data["Date"])[
                "time_in_millis"
            ]
                price = data["Close"]
            
            ticker_price_map.append(
                {"entry_time_in_millis": entry_time_in_ms, "fmv": price}
            )

        # Sort by date for efficient lookup
        ticker_price_map.sort(key=lambda x: x["entry_time_in_millis"])
        price_map_cache[ticker] = ticker_price_map

    return price_map_cache[ticker]


def get_fmv(ticker: str, purchase_time_in_ms: int) -> float:
    logger.debug_log(
        f"{ticker}: Querying FMV at {date_utils.display_time(purchase_time_in_ms)}"
    )

    previous_entry_data = None
    for entry_data in __init_map(ticker):
        entry_time_in_ms = entry_data["entry_time_in_millis"]
        if entry_time_in_ms >= purchase_time_in_ms:
            if entry_time_in_ms > purchase_time_in_ms:
                previous_entry_time_in_ms = previous_entry_data["entry_time_in_millis"]
                __validate_dates(
                    previous_entry_time_in_ms, purchase_time_in_ms, entry_time_in_ms
                )
                return entry_data["fmv"]
            return entry_data["fmv"]

        previous_entry_data = entry_data
    
    # Updated error message to reflect new file locations
    if ticker.lower() == "adbe":
        ticker_share_price = "historic_data/adobe_price_history.csv"
    else:
    ticker_share_price = os.path.join("historic_data", "shares", ticker, "data.csv")
    
    raise AssertionError(
        f"No FMV data for share ticker {ticker} in {ticker_share_price} for date "
        + f"{date_utils.log_timestamp(purchase_time_in_ms)}"
    )


def get_usd_inr_rate(time_in_ms: int) -> float:
    """
    Get USD to INR exchange rate for a given timestamp
    """
    usd_inr_data = __init_usd_inr_map()
    
    previous_entry_data = None
    for entry_data in usd_inr_data:
        entry_time_in_ms = entry_data["entry_time_in_millis"]
        if entry_time_in_ms >= time_in_ms:
            if entry_time_in_ms > time_in_ms:
                if previous_entry_data:
                    return previous_entry_data["fmv"]
                else:
                    return entry_data["fmv"]
            return entry_data["fmv"]
        previous_entry_data = entry_data
    
    # If no future data found, use the last available rate
    if previous_entry_data:
        return previous_entry_data["fmv"]
    
    raise AssertionError(
        f"No USD/INR rate data available for date "
        + f"{date_utils.log_timestamp(time_in_ms)}"
    )


def get_closing_price(ticker: str, end_time_in_ms: int) -> float:
    price_map = list(
        filter(
            lambda price: price["entry_time_in_millis"] <= end_time_in_ms,
            sorted(
                __init_map(ticker),
                key=lambda price: price["entry_time_in_millis"],
                reverse=True,
            ),
        )
    )

    return price_map[0]["fmv"]


def get_peak_price_in_inr(
    ticker: str, start_time_in_ms: int, end_time_in_ms: int
) -> float:
    if start_time_in_ms > end_time_in_ms:
        raise AssertionError(
            f"start_time_in_ms = {start_time_in_ms} is greater "
            + f"than equal to end_time_in_ms = {end_time_in_ms}"
        )

    price_map = list(
        filter(
            lambda price: price["entry_time_in_millis"] <= end_time_in_ms
            and price["entry_time_in_millis"] >= start_time_in_ms,
            sorted(
                __init_map(ticker),
                key=lambda price: price["entry_time_in_millis"],
                reverse=True,
            ),
        )
    )
    
    # Use the new USD/INR rate function or fallback to RBI rates
    price_map_with_inr_rate: t.Iterator[TimedFmvWithInrRate] = map(
        lambda price: {
            **price,
            "inr_rate": get_usd_inr_rate(price["entry_time_in_millis"])
            if ticker_currency_info[ticker] == "USD"
            else rbi_rates_utils.get_rate_for_prev_mon_for_time_in_ms(
                ticker_currency_info[ticker], price["entry_time_in_millis"]
            ),
        },
        price_map,
    )

    max_value = max(
        price_map_with_inr_rate, key=lambda price: price["fmv"] * price["inr_rate"]
    )

    peak_price_in_inr = max_value["fmv"] * max_value["inr_rate"]

    logger.debug_log_json(
        {
            "start_time": date_utils.display_time(start_time_in_ms),
            "end_time": date_utils.display_time(end_time_in_ms),
            "max_fmv($)": max_value["fmv"],
            "max_fmv($)_at": date_utils.display_time(max_value["entry_time_in_millis"]),
            "inr_conversion_rate": max_value["inr_rate"],
            "effective_price(INR)": peak_price_in_inr,
        }
    )

    logger.log(
        f"Peak price for ticker = {ticker} from {date_utils.display_time(start_time_in_ms)} "
        + f"to {date_utils.display_time(end_time_in_ms)} is {peak_price_in_inr} "
        + f"INR at rate {max_value['inr_rate']} INR/USD"
    )

    return max_value["fmv"] * max_value["inr_rate"]
