package com.company;

import org.json.JSONArray;

import java.util.List;

public interface GetRequestCallback {
  void responseHandler(String pair, List<JSONArray> history);
}
