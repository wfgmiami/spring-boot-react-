import React from 'react';
import ReactDataGrid from 'react-data-grid';

class BucketAllocation extends React.Component{
	constructor(props){
		super(props);

		this.state = {
			columns: [],
			bucketsByRows: []
		}
		this.rowGetter = this.rowGetter.bind(this);
	  // this.rowStyle = this.rowStyle.bind(this);
	}

	componentWillMount(){
		this.setState({ columns: this.props.columns });
		this.setState({ bucketsByRows: this.props.bucketsByRows });
	}

	componentWillReceiveProps( nextProps ){
		console.log('bucket allocation...next Props', nextProps);
		if( nextProps.bucketsByRows !== this.state.bucketsByRows ){
			this.setState( { bucketsByRows: nextProps.bucketsByRows } );
			this.setState( { columns: nextProps.columns } );
		}
	}

	rowGetter( i ){
//		console.log('row function.....', this.state.bucketsByRows[i],i, this.state.bucketsByRows);
		return this.state.bucketsByRows[i];
	}


	render(){
  		const total = this.state.bucketsByRows.length;
		const headerText = "BUCKETS ALLOCATION";
console.log('....buckets allocation render this.state.bucketsByRows',this.state.props, this.state.bucketsByRows);
		return (
			<div className="panel panel-default"><div style={{ textAlign: 'center' }}><b>{ headerText }</b></div>
			<div>&nbsp;&nbsp;</div>
			<ReactDataGrid
				columns = { this.state.columns }
				rowGetter = { this.rowGetter }
				rowsCount = { total }
				minHeight = { 450 }
				minColumnWidth = { 150 }

				/>
			</div>
		);

	}

}

export default BucketAllocation;