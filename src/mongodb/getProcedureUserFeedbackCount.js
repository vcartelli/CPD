db.models.aggregate(

	// Pipeline
	[
		// Stage 1
		{
			$match: {
			  "＄domain": "Model.FPMN.Diagram"
			}
		},

		// Stage 2
		{
			$project: {
			  "_id": 1,
			  "diagram": "$$ROOT"
			}
		},

		// Stage 3
		{
			$lookup: {
			    from: "diagrams",
			    localField: "diagram.planeId",
			    foreignField: "_id",
			    as: "procedure"
			}
		},

		// Stage 4
		{
			$unwind: "$procedure"
		},

		// Stage 5
		{
			$lookup: {
			    from: "models",
			    localField: "procedure.modelId",
			    foreignField: "_id",
			    as: "procedure"
			}
		},

		// Stage 6
		{
			$match: {
			  // {procedureId}
			  "procedure.＄domain": "Model.FPMN.Procedure"
			}
		},

		// Stage 7
		{
			$unwind: "$procedure"
		},

		// Stage 8
		{
			$lookup: {
			    from: "user.feedbacks",
			    localField: "_id",
			    foreignField: "coordinates.diagramId",
			    as: "feedbacks"
			}
		},

		// Stage 9
		{
			$unwind: "$feedbacks"
		},

		// Stage 10
		{
			$group: {
			    "_id": "$procedure._id",
			    "count": {
			      $sum: 1
			    }
			}
		},

	]

	// Created with Studio 3T, the IDE for MongoDB - https://studio3t.com/

);
