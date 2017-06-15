package crabzilla.stack.model;

import crabzilla.model.ProjectionData;

import java.util.List;

public interface EventsProjector {

  String getEventsChannelId();

  Long getLastUowSeq();

  void handle(List<ProjectionData> uowList);

}