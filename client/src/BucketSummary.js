import React from 'react';
import ReactDataGrid  from 'react-data-grid';

class BucketSummary extends React.Component{
	constructor(props){
		super(props);

		this.state = {
			bucketsSummary: []
		}

		this._columns = [
			{ key: 'portfolioSummary', name: 'Portfolio Summary', resizable: true },
			{ key: 'dollarAllocated', name: 'Dollar Allocation', resizable: true },
			{ key: 'percentageAllocated', name: 'Percentage Allocated', resizable: true },
			{ key: 'rule', name: 'Rule', resizable: true },
		]
		this.rowGetter = this.rowGetter.bind(this);
	}

	componentWillMount(){
		console.log('.....allocation summary willMount this.props', this.props);
		this.setState({ bucketsSummary: this.props.bucketsSummary });
	}

	componentWillReceiveProps( nextProps ){
		console.log('allocation summary...next Props', nextProps);
		if( nextProps.bucketsSummary !== this.state.bucketsSummary ){
			this.setState( { bucketsSummary: nextProps.bucketsSummary } );
		}
	}

	rowGetter( i ){
		return this.state.bucketsSummary[i];
	}

	render(){

		const total = this.state.bucketsSummary.length;
		const headerText = "ALLOCATION SUMMARY";
	//	console.log('.....muni list', this.state.munis);
		return (
			<div className="panel panel-default"><div style = {{ textAlign: 'center' }}><b>{ headerText }</b></div>
			<div>&nbsp;&nbsp;</div>
			<ReactDataGrid
				columns={ this._columns }
				rowGetter = { this.rowGetter }
				rowsCount = { total }
				minHeight = { 450 }
				/>
			</div>
		);

	}
}


export default BucketSummary;