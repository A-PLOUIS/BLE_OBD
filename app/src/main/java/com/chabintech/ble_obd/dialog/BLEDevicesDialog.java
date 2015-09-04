package com.chabintech.ble_obd.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BLEDevicesDialog.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BLEDevicesDialog#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BLEDevicesDialog extends DialogFragment {

    private static final String ARG_DEVICES = "devicesList";

    private ArrayList<BluetoothDevice> mDevices;

    private OnFragmentInteractionListener mListener;

    public BLEDevicesDialog() {
        // Required empty public constructor
    }

    /**
     * Factory method to create an instance of BLEDevicesDialog
     *
     * @param p_devices Set of available BLE devices.
     * @return A new instance of fragment BLEDevicesDialog.
     */
    // TODO: Rename and change types and number of parameters
    public static BLEDevicesDialog newInstance(Set<BluetoothDevice> p_devices) {
        BLEDevicesDialog fragment = new BLEDevicesDialog();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_DEVICES, new ArrayList<>(p_devices));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDevices = getArguments().getParcelableArrayList(ARG_DEVICES);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Paired devices");
        builder.setAdapter(new ListDevicesAdapter(getActivity(),
                        android.R.layout.simple_list_item_1,
                        mDevices),
                null);

        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void connectToBLEDevice(BluetoothDevice p_device);
    }

    private class ListDevicesAdapter extends ArrayAdapter<BluetoothDevice> {

        public ListDevicesAdapter(Context context, int resource, List<BluetoothDevice> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity()).inflate(android.R.layout.simple_list_item_1, null);
            }
            final BluetoothDevice currentItem = getItem(position);

            ((TextView) convertView.findViewById(android.R.id.text1)).setText(currentItem.getName());
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                    if (adapter != null) {
                        adapter.cancelDiscovery();
                        mListener.connectToBLEDevice(currentItem);
                    }
                }
            });


            return convertView;
        }
    }

}
